# Channel attribution screenshots

These screenshots show the production Thread Viewer components at commit
`f137b83b65506e80edbc41b77e98d502fd941477`, rendered with recorded snapshots
from the isolated OIE 4.6.0 validation run on September 22, 2026. The channels
are synthetic test fixtures. These are component captures replaying recorded
data, not screenshots of a currently connected administrator session.

- [Swing channel ownership](swing-channel-ownership.png): stable channel and
  association filters identify the idle polling worker and scheduler. The
  selected worker shows its channel ID, source connector ID, resolution reason,
  and lifetime CPU totals.
- [Web connector identity](web-connector-identity.png): `Send (1) Backup` stays
  intact as the connector name, separate from connector metadata ID `1`.
  Details expose the channel ID, association, resolution, and match reason.
- [Web refreshed details](web-refreshed-channel-details.png): an open dialog
  follows worker `101` from `TV Review Destination Queued` to `TV Review VM Slow`
  between two recorded samples. The screenshot shows the later sample, including
  its differing saved name, `Saved Rename Without Redeploy`.

The web captures use the shipped React bundle, the host's actual Radix dialog
renderer and CSS, and Chromium at 2× pixel density. REST and routing are fixture
boundaries; the captured thread data is unchanged. The Swing capture renders the
actual compiled panel with the standard Swing Metal look and feel and a retained
snapshot after monitoring stops. All 27 loaded plugin classes match the validated
plugin ZIP. Neither capture requires a new engine or database.

Capture inputs (SHA-256):

| Input | SHA-256 |
| --- | --- |
| Shipped web bundle | `06c5dbef1c66b7374fabfa9ad3884a65551070cbaf0b539fbe252c92011d702a` |
| Active JSON snapshot | `74c3747a3ceec9f8dab09cd5849a03eb1943b3502d683fb1c85474ab58ab57d5` |
| Saved-rename JSON snapshot | `e0ec88e1ceeb9a8e2e40d8e145454747d82b1c63c6d3dcc16748c3d95fdc2e4b` |

The capture harnesses, original snapshots, assertions, and image hashes are
retained with the local validation evidence. The replay asserts that the web
detail dialog keeps the same thread ID while updating its current channel and
saved name, with no browser errors.
