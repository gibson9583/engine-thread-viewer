import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import { asList } from './wire-fixture.mjs';

// Run the actual served bundle, substituting only the public host boundary.
const compiled = readFileSync(new URL('../web/plugin.js', import.meta.url), 'utf8');
const script = compiled.replace(/import \{ platform \} from "@oie\/web-shell";/, 'const platform = globalThis.platform;')
    .replace(/export \{\s*register\s*\};?\s*$/, '')
    + '\nglobalThis.subject = { store, emit, ThreadViewerTab, ThreadRow, showDetail, copyText, normalizeSnapshot, buildThreadDump };';

function fixture(widths = null) {
    const toasts = [], dialogs = [];
    const node = (type, props, ...children) => ({ type, props: props || {}, children: children.flat(),
        replaceChildren(...next) { this.children = next; } });
    const platform = {
        React: { useReducer: () => [0, () => {}], useEffect: () => {}, createElement: node },
        api: { asList },
        ui: { h: node, toast: (...args) => toasts.push(args), modal: options => {
            dialogs.push(options);
            return { close: () => options.onClose?.() };
        } }
    };
    const navigator = {};
    const context = vm.createContext({ platform, navigator, localStorage: { getItem: () => widths },
        document: { getElementById: () => ({}) }, Date, setTimeout, clearTimeout });
    vm.runInContext(script, context);
    return { ...context.subject, dialogs, toasts, navigator };
}

const raw = (name = 'Original', channelId = 'channel-1') => ({ timestamp: 1234,
    threads: [{ threadId: 90, name, channelId, channelName: name, state: 'RUNNABLE',
        role: 'Writer', associationKind: 'execution', resolutionStatus: 'matched', matchReason: 'Validated ID',
        connectorName: 'Send (1) Backup', connectorMetadataId: 1, stackTrace: { string: ['test.Class.run(Class.java:1)'] } }],
    channels: [{ id: channelId, name, deployed: true }] });
const collect = (tree, predicate) => {
    const matches = [];
    if (tree && typeof tree === 'object') {
        if (predicate(tree)) matches.push(tree);
        for (const child of tree.children || []) matches.push(...collect(child, predicate));
    }
    return matches;
};
const textContent = tree => typeof tree === 'string' ? tree : tree?.children?.map(textContent).join(' ') || '';

test('compiled table exposes stable filter IDs, keyboard sorting and horizontal scroll', () => {
    const f = fixture('{"name":5000,"state":"bad","cpu":-1}');
    f.store.snapshot = f.normalizeSnapshot(raw());
    f.store.filters.channel = 'channel-1';
    const tree = f.ThreadViewerTab();
    const channel = collect(tree, n => n.type === 'select' && n.props['aria-label'] === 'Filter by channel')[0];
    assert.equal(channel.props.value, 'channel-1');
    assert.equal(collect(channel, n => n.type === 'option')[1].props.value, 'channel-1');
    assert.ok(collect(tree, n => n.type === 'button' && n.props['aria-label'] === 'Sort by Role').length);
    assert.ok(collect(tree, n => n.props.className === 'flex-1 min-h-0 overflow-auto').length);
    const table = collect(tree, n => n.type === 'table')[0];
    assert.ok(Number.parseInt(table.props.style.minWidth, 10) > 1000);
    assert.equal(f.store.colWidths.name, 2000);
    assert.equal(f.store.colWidths.cpu, 50);
    assert.equal(f.store.colWidths.state, undefined);
    const row = f.ThreadRow({ t: f.store.snapshot.threads[0] });
    assert.equal(row.props.tabIndex, 0);
    let prevented = false;
    const rowElement = {};
    row.props.onKeyDown({ key: 'Enter', target: rowElement, currentTarget: rowElement,
        preventDefault: () => { prevented = true; } });
    assert.equal(prevented, true);
    assert.equal(f.dialogs.length, 1);
    f.dialogs[0].onClose();
});

test('visible Details button opens once without bubbling keyboard activation to the row', () => {
    const f = fixture();
    f.store.snapshot = f.normalizeSnapshot(raw());
    const row = f.ThreadRow({ t: f.store.snapshot.threads[0] });
    const details = collect(row, n => n.type === 'button' && n.children.includes('Details'))[0];
    assert.ok(details);
    assert.equal(details.props['aria-label'], 'Details for thread Original');
    let stopped = false;
    const rowElement = {}, buttonElement = {};
    row.props.onKeyDown({ key: 'Enter', target: buttonElement, currentTarget: rowElement,
        preventDefault: () => assert.fail('Row must not handle the button key event') });
    assert.equal(f.dialogs.length, 0);
    details.props.onClick({ stopPropagation: () => { stopped = true; } });
    assert.equal(stopped, true);
    assert.equal(f.dialogs.length, 1);
    stopped = false;
    details.props.onDoubleClick({ stopPropagation: () => { stopped = true; } });
    assert.equal(stopped, true);
    assert.equal(f.dialogs.length, 1);
    f.dialogs[0].onClose();
});

test('detail dialog follows reassignment by thread ID, copies the displayed sample and releases its listener', async () => {
    const f = fixture();
    let copied;
    f.navigator.clipboard = { writeText: async text => { copied = text; } };
    f.store.monitoring = true;
    f.store.snapshot = f.normalizeSnapshot(raw());
    f.showDetail(f.store.snapshot.threads[0]);
    const dialog = f.dialogs[0];
    assert.match(textContent(dialog.body), /Original/);
    assert.equal(f.store.listeners.size, 1);
    f.store.snapshot = f.normalizeSnapshot(raw('New assignment', 'channel-2'));
    f.emit();
    assert.match(textContent(dialog.body), /New assignment/);
    assert.doesNotMatch(textContent(dialog.body), /Original/);
    await dialog.buttons[0].onClick();
    assert.match(copied, /Channel: New assignment \[channel-2\]/);
    f.store.snapshot = f.normalizeSnapshot({ timestamp: 2234, threads: [] });
    f.emit();
    assert.match(textContent(dialog.body), /Thread no longer present/);
    await dialog.buttons[0].onClick();
    assert.match(copied, /Thread no longer present; last captured sample/);
    assert.match(copied, /Channel: New assignment \[channel-2\]/);
    dialog.onClose();
    assert.equal(f.store.listeners.size, 0);
});

test('clipboard feedback awaits success and reports asynchronous rejection', async () => {
    const f = fixture();
    let reject;
    f.navigator.clipboard = { writeText: () => new Promise((resolve, fail) => { reject = fail; }) };
    const pending = f.copyText('sample');
    assert.equal(f.toasts.length, 0);
    reject(new Error('Permission denied'));
    await pending;
    assert.equal(f.toasts.length, 1);
    assert.match(f.toasts[0][0], /Could not copy.*Permission denied/);
    assert.equal(f.toasts[0][1], 'warn');
});

test('attribution details do not alter jstack export format', () => {
    const f = fixture();
    const s = f.normalizeSnapshot(raw());
    s.threads[0].jstackDump = '"Original" #90\n   java.lang.Thread.State: RUNNABLE\n\tat test.Class.run(Class.java:1)\n';
    const dump = f.buildThreadDump(s);
    assert.ok(dump.includes(s.threads[0].jstackDump));
    assert.doesNotMatch(dump, /Association:|Match reason:|Saved channel name:/);
    assert.match(dump, /Found 0 deadlocks/);
});
