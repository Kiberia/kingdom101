# Match protocol

Matchmaking is unchanged: the client sends `/app/join` and `/app/queue/ping`. The server replies on `/topic/room-assign/{playerId}`.

```json
{
  "roomId": "room_ab12cd34",
  "players": [{ "playerId": "p", "name": "ada", "playerNumber": 1 }],
  "minX": 0,
  "maxX": 5,
  "minY": -3,
  "maxY": 3,
  "cell": 0.5,
  "streamKey": "secret",
  "vsBot": false
}
```

Player 1 is at (0, 0). Player 2 is at (5, 0). A unit is a circle of radius 0.05. It moves in a straight line from point A to point B.

- Player 1: A is `(0, ay)` and B is `(5, by)`. `ay` and `by` are chosen independently in [-3, 3].
- Player 2: A is `(5, ay)` and B is `(0, by)`, with the same y range.

Each server step moves the center 0.05 toward B, then publishes coordinates on `/topic/room/{roomId}/{streamKey}`. The field is split into squares of 0.5. A circle is compared only with circles in its own square and the eight squares that touch it.

If two circles touch, both are removed (`collision`). A circle that reaches B is removed (`exit`). The side that still has a circle when the other has none wins. If both sides lose their last circles on the same step, the match is a draw.

Each message is a full snapshot. Replace local circles with `units`. `removed` is only what disappeared on that step. `vx` and `vy` are the displacement of one full step.

```json
{
  "phase": "BATTLE",
  "step": 3,
  "minX": 0,
  "maxX": 5,
  "minY": -3,
  "maxY": 3,
  "cell": 0.5,
  "units": [
    { "id": "p1-1", "player": 1, "radius": 0.05, "x": 0.15, "y": 0.4, "vx": 0.05, "vy": 0.0, "destX": 5, "destY": 1.2 }
  ],
  "removed": [
    { "id": "p2-2", "player": 2, "reason": "collision", "x": 2.5, "y": 0.0 }
  ],
  "winner": null,
  "matchResult": null
}
```

`phase` is `BATTLE` or `GAMEOVER`. `winner` is `"1"`, `"2"`, or `"draw"`. On `GAMEOVER`, `matchResult` is `WIN`, `LOSS`, or `DRAW` for the player who holds that stream key. `wins`, `losses`, and `elo` are included when that player has an account.

Field numbers live in `shared/field.json`.
