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
        ui: { h: (type, attrs, ...children) => {
            // The host DOM helper accepts h(tag, text/node/children) as well as
            // h(tag, attributes, ...children); JSX createElement does not.
            if (attrs && (typeof attrs !== 'object' || Array.isArray(attrs) || Array.isArray(attrs.children))) {
                return node(type, null, attrs, ...children);
            }
            return node(type, attrs, ...children);
        }, fmtDate: millis => `Host timestamp ${millis}`, toast: (...args) => toasts.push(args), modal: options => {
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
const detailsButton = f => {
    const buttons = collect(f.ThreadViewerTab(), n => n.type === 'button' && n.children.includes('Details'));
    assert.equal(buttons.length, 1, 'There is one Details action in the toolbar');
    return buttons[0];
};
const keyEvent = (key, extra = {}) => {
    const target = {};
    return { key, target, currentTarget: target, preventDefault() {}, ...extra };
};

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

test('click and Space select a row; the single toolbar Details action opens the selected thread', () => {
    const f = fixture();
    f.store.snapshot = f.normalizeSnapshot(raw());
    const row = f.ThreadRow({ t: f.store.snapshot.threads[0] });
    assert.equal(collect(row, n => n.type === 'button').length, 0);
    assert.equal(row.props['aria-selected'], false);
    assert.equal(detailsButton(f).props.disabled, true);
    row.props.onClick();
    assert.equal(f.store.selectedThreadId, 90);
    assert.equal(f.dialogs.length, 0);
    const selected = f.ThreadRow({ t: f.store.snapshot.threads[0] });
    assert.equal(selected.props['aria-selected'], true);
    assert.match(selected.props.className, /\bselected\b/);
    assert.equal(detailsButton(f).props.disabled, false);
    detailsButton(f).props.onClick();
    assert.equal(f.dialogs.length, 1);
    assert.match(f.dialogs[0].title, /id=90/);
    f.dialogs[0].onClose();
    f.store.selectedThreadId = null;
    let prevented = false;
    row.props.onKeyDown(keyEvent(' ', { preventDefault: () => { prevented = true; } }));
    assert.equal(prevented, true);
    assert.equal(f.store.selectedThreadId, 90);
    assert.equal(f.dialogs.length, 1);
});

test('double-click and Enter select and activate once; key repeats and descendant events do not activate', () => {
    const f = fixture();
    f.store.snapshot = f.normalizeSnapshot(raw());
    const row = f.ThreadRow({ t: f.store.snapshot.threads[0] });
    let selectionRenders = 0;
    const onSelection = () => { selectionRenders++; };
    f.store.listeners.add(onSelection);
    row.props.onClick();
    row.props.onClick();
    assert.equal(f.dialogs.length, 0);
    row.props.onDoubleClick();
    assert.equal(f.dialogs.length, 1);
    assert.equal(f.store.selectedThreadId, 90);
    assert.equal(selectionRenders, 1, 'Double-click only redraws selection once');
    f.store.listeners.delete(onSelection);
    f.dialogs[0].onClose();
    f.store.selectedThreadId = null;
    row.props.onKeyDown(keyEvent('Enter', { repeat: true }));
    row.props.onKeyDown(keyEvent('Enter', { target: {},
        preventDefault: () => assert.fail('Ignore descendant key events') }));
    assert.equal(f.dialogs.length, 1);
    row.props.onKeyDown(keyEvent('Enter'));
    assert.equal(f.dialogs.length, 2);
    assert.equal(f.store.selectedThreadId, 90);
    f.dialogs[1].onClose();
    assert.equal(f.store.listeners.size, 0);
});

test('selection survives sorting, reassignment, stopped monitoring and transient errors; actions use current state', () => {
    const f = fixture();
    f.store.snapshot = f.normalizeSnapshot(raw());
    const staleRow = f.ThreadRow({ t: f.store.snapshot.threads[0] });
    staleRow.props.onClick();
    const staleButton = detailsButton(f);
    const next = raw('Reassigned', 'channel-2');
    next.threads.push({ ...raw('Other').threads[0], threadId: 91 });
    f.store.snapshot = f.normalizeSnapshot(next);
    f.store.error = 'Temporary disconnect';
    f.emit();
    const tree = f.ThreadViewerTab();
    collect(tree, n => n.type === 'button' && n.props['aria-label'] === 'Sort by Thread Name')[0].props.onClick();
    assert.equal(f.store.selectedThreadId, 90);
    assert.equal(detailsButton(f).props.disabled, false);
    staleRow.props.onDoubleClick();
    assert.match(textContent(f.dialogs[0].body), /Reassigned/);
    assert.doesNotMatch(textContent(f.dialogs[0].body), /Original/);
    f.dialogs[0].onClose();
    f.ThreadRow({ t: f.store.snapshot.threads[1] }).props.onClick();
    staleButton.props.onClick();
    assert.match(f.dialogs[1].title, /id=91/);
    f.dialogs[1].onClose();
});

test('filtering, disappearance and an unavailable plugin clear selection without later resurrection', () => {
    for (const change of ['filter', 'removed', 'not-installed', 'no-snapshot']) {
        const f = fixture();
        f.store.snapshot = f.normalizeSnapshot(raw());
        f.ThreadRow({ t: f.store.snapshot.threads[0] }).props.onClick();
        if (change === 'filter') {
            const search = collect(f.ThreadViewerTab(), n => n.type === 'input')[0];
            search.props.onChange({ target: { value: 'no matching thread' } });
        } else {
            if (change === 'removed') f.store.snapshot = f.normalizeSnapshot({ threads: [] });
            if (change === 'not-installed') f.store.notInstalledStatus = 404;
            if (change === 'no-snapshot') f.store.snapshot = null;
            f.emit();
        }
        assert.equal(f.store.selectedThreadId, null, change);
        assert.equal(detailsButton(f).props.disabled, true, change);
        f.store.filters.search = '';
        f.store.notInstalledStatus = null;
        f.store.snapshot = f.normalizeSnapshot(raw());
        f.emit();
        assert.equal(detailsButton(f).props.disabled, true, change);
        assert.equal(f.dialogs.length, 0);
    }
});

test('stale row and toolbar callbacks cannot open a thread that is gone or hidden before render', () => {
    for (const change of ['filter', 'removed', 'not-installed', 'reassigned-outside-filter']) {
        const f = fixture();
        f.store.snapshot = f.normalizeSnapshot(raw());
        f.store.filters.channel = 'channel-1';
        const staleRow = f.ThreadRow({ t: f.store.snapshot.threads[0] });
        staleRow.props.onClick();
        const staleButton = detailsButton(f);
        if (change === 'filter') f.store.filters.search = 'no matching thread';
        if (change === 'removed') f.store.snapshot = f.normalizeSnapshot({ threads: [] });
        if (change === 'not-installed') f.store.notInstalledStatus = 404;
        if (change === 'reassigned-outside-filter') f.store.snapshot = f.normalizeSnapshot(raw('New channel', 'channel-2'));
        // Deliberately do not emit/re-render: handlers must re-check action-time state.
        staleButton.props.onClick();
        staleRow.props.onDoubleClick();
        staleRow.props.onKeyDown(keyEvent('Enter'));
        assert.equal(f.dialogs.length, 0, change);
        assert.equal(detailsButton(f).props.disabled, true, change);
    }
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
    assert.match(textContent(dialog.body), /Captured Host timestamp 1234/);
    assert.equal(f.store.listeners.size, 1);
    f.store.snapshot = f.normalizeSnapshot({ ...raw('New assignment', 'channel-2'), timestamp: 2234 });
    f.emit();
    assert.match(textContent(dialog.body), /New assignment/);
    assert.doesNotMatch(textContent(dialog.body), /Original/);
    assert.match(textContent(dialog.body), /Captured Host timestamp 2234/);
    await dialog.buttons[0].onClick();
    assert.match(copied, /Channel: New assignment \[channel-2\]/);
    f.store.snapshot = f.normalizeSnapshot({ timestamp: 3234, threads: [] });
    f.emit();
    assert.match(textContent(dialog.body), /Thread no longer present/);
    assert.match(textContent(dialog.body), /Captured Host timestamp 2234/);
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
