import test from 'node:test';
import assert from 'node:assert/strict';
import { decodeSnapshot as normalizeSnapshot, channelOptions, categoryOptions, filterThreads,
    detailText } from '../web/thread-model.js';

import { asList } from './wire-fixture.mjs';

const id = 'e9222026-0922-4000-8000-000000000001';
const otherId = 'e9222026-0922-4000-8000-000000000002';
const thread = (changes = {}) => ({ threadId: 90, name: 'JavaScript Task', state: 'TIMED_WAITING',
    channelId: id, channelName: 'Deployed name', savedChannelName: 'Saved rename',
    connectorName: 'Send (1) Backup', connectorMetadataId: 1, role: 'JavaScript Writer JavaScript Task',
    associationKind: 'execution', resolutionStatus: 'matched', matchReason: 'Deployed channel and connector metadata match',
    cpuTimeNanos: 1200000, userTimeNanos: -1, stackTrace: { string: 'java.lang.Thread.sleep(Native Method)' }, ...changes });
const snapshot = (threads = [thread()], channels = [{ id, name: 'Deployed name', savedName: 'Saved rename', deployed: true }]) =>
    normalizeSnapshot({ threads, channels, timestamp: 1234 }, asList);

test('decodes actual unaliased singleton and multirow channel/thread wire shapes', () => {
    for (const many of [false, true]) {
        const t = thread({ connectorMetadataId: '0', cpuTimeNanos: '1200000', daemon: { boolean: true } });
        const c = { id: id.toUpperCase(), name: 'Deployed name', savedName: 'Saved rename', deployed: 'true' };
        const s = normalizeSnapshot({
            threads: { 'com.mirth.connect.plugins.threadviewer.shared.ThreadInfo': many ? [t, t] : t },
            channels: { 'com.mirth.connect.plugins.threadviewer.shared.ChannelInfo': many ? [c, c] : c }
        }, asList);
        assert.equal(s.threads.length, many ? 2 : 1);
        assert.equal(s.channels.length, many ? 2 : 1);
        assert.equal(s.channels[0].id, id);
        assert.equal(s.channels[0].deployed, true);
        assert.equal(s.threads[0].connectorMetadataId, 0);
        assert.equal(s.threads[0].daemon, true);
        assert.deepEqual(s.threads[0].stackTrace, ['java.lang.Thread.sleep(Native Method)']);
    }
});

test('legacy responses derive stable ID options without inventing association metadata', () => {
    const s = normalizeSnapshot({ threads: { threadInfo: { name: 'Legacy thread', channelId: id,
        channelName: 'Legacy channel', stackTrace: {} } }, deployedChannelNames: { string: 'Legacy channel' } }, asList);
    assert.equal(s.threads[0].associationKind, 'unknown');
    assert.equal(s.threads[0].resolutionStatus, 'unknown');
    assert.equal(s.threads[0].connectorMetadataId, null);
    assert.equal(s.threads[0].cpuMs, -1);
    assert.deepEqual(s.threads[0].stackTrace, []);
    assert.equal(channelOptions(s)[0].id, id);
    assert.equal(filterThreads(s, { channel: id }).length, 1);
    assert.equal(filterThreads(s, { association: 'execution' }).length, 0);
    assert.match(detailText(s.threads[0]), /Not reported by this server/);
});

test('channel filtering survives rename and does not conflate duplicate names', () => {
    const s = snapshot([thread({ channelName: 'Renamed' }), thread({ threadId: 91, channelId: otherId, channelName: 'Renamed' })],
        [{ id, name: 'Renamed' }, { id: otherId, name: 'Renamed' }]);
    const options = channelOptions(s, id);
    assert.deepEqual(options.map(c => c.id), [id, otherId]);
    assert.notEqual(options[0].label, options[1].label);
    assert.equal(filterThreads(s, { channel: id })[0].threadId, 90);
    assert.equal(filterThreads(s, { channel: id }).length, 1);
});

test('metadata includes idle channels; missing or removed selections remain visible', () => {
    const s = snapshot([], [{ id, name: 'Idle' }]);
    assert.equal(channelOptions(s, id)[0].label, 'Idle');
    const missing = channelOptions(s, otherId).find(c => c.id === otherId);
    assert.equal(missing.missing, true);
    assert.match(missing.label, /not in this snapshot/);
    assert.equal(filterThreads(s, { channel: otherId }).length, 0);
    assert.equal(channelOptions(null, id)[0].id, id);
});

test('association filters follow current snapshots when a shared worker changes channels and goes idle', () => {
    const current = snapshot([thread(), thread({ threadId: 91, channelId: otherId, associationKind: 'ownership' })]);
    assert.deepEqual(filterThreads(current, { association: 'execution', channel: id }).map(t => t.threadId), [90]);
    const next = snapshot([thread({ channelId: otherId })]);
    assert.equal(filterThreads(next, { channel: id }).length, 0);
    assert.equal(filterThreads(next, { channel: otherId }).length, 1);
    const idle = snapshot([thread({ channelId: null, channelName: null, savedChannelName: null,
        associationKind: 'unassigned', category: 'Executor' })]);
    assert.equal(filterThreads(idle, { channel: otherId }).length, 0);
    assert.equal(filterThreads(idle, { association: 'unassigned' }).length, 1);
});

test('search covers full connector names, saved names, roles and frames; categories accept new server values', () => {
    const s = snapshot([thread({ category: 'Future category' })]);
    for (const search of ['Send (1) Backup', 'Saved rename', 'Writer JavaScript', 'Thread.sleep']) {
        assert.equal(filterThreads(s, { search }).length, 1, search);
    }
    assert.ok(categoryOptions(s).includes('Channel Management'));
    assert.ok(categoryOptions(s).includes('Executor'));
    assert.ok(categoryOptions(s).includes('Future category'));
});

test('copied details preserve attribution, deployed/saved mismatch and unavailable lifetime metrics', () => {
    const text = detailText(snapshot().threads[0]);
    assert.match(text, /Lifetime thread CPU: 1.2 ms/);
    assert.match(text, /Lifetime thread user time: Unavailable/);
    assert.match(text, /including work for previous channels/);
    assert.match(text, /Connector: Send \(1\) Backup \[metadata ID=1\]/);
    assert.match(text, /Role: JavaScript Writer JavaScript Task/);
    assert.match(text, /Saved channel name: Saved rename \(differs from captured channel name\)/);
    assert.match(text, /Resolution: matched/);
    assert.match(text, /Match reason: Deployed channel and connector metadata match/);
});
