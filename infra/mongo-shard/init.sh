#!/usr/bin/env bash
# Wire up the sharded cluster:
#   1. initiate cfgrs on the config server
#   2. initiate shard1rs / shard2rs on each shard
#   3. add both shards to the cluster via the mongos router
#   4. enable sharding on the demo db and shard one collection
#
# Idempotent — re-running is safe (rs.initiate / sh.addShard yell on the
# second call but we tolerate the error).
set -uo pipefail

wait_for() {
  local host="$1" port="$2"
  echo "==> waiting for $host:$port"
  for _ in $(seq 1 60); do
    if mongosh --quiet --host "$host" --port "$port" --eval "db.runCommand({ping:1}).ok" 2>/dev/null | grep -q 1; then
      echo "    ready"; return 0
    fi
    sleep 2
  done
  echo "!! $host:$port never came up"; return 1
}

wait_for cfg    27019
wait_for shard1 27018
wait_for shard2 27028
wait_for mongos 27020

echo "==> initiating cfgrs"
mongosh --quiet --host cfg --port 27019 --eval '
  try {
    rs.initiate({ _id: "cfgrs", configsvr: true, members: [{ _id: 0, host: "cfg:27019" }] });
  } catch (e) { print("cfgrs init: " + e.message); }
'

echo "==> initiating shard1rs"
mongosh --quiet --host shard1 --port 27018 --eval '
  try {
    rs.initiate({ _id: "shard1rs", members: [{ _id: 0, host: "shard1:27018" }] });
  } catch (e) { print("shard1rs init: " + e.message); }
'

echo "==> initiating shard2rs"
mongosh --quiet --host shard2 --port 27028 --eval '
  try {
    rs.initiate({ _id: "shard2rs", members: [{ _id: 0, host: "shard2:27028" }] });
  } catch (e) { print("shard2rs init: " + e.message); }
'

# Replica sets need a beat to elect a primary before mongos will accept addShard
sleep 8

echo "==> adding shards to cluster via mongos"
mongosh --quiet --host mongos --port 27020 --eval '
  sh.addShard("shard1rs/shard1:27018");
  sh.addShard("shard2rs/shard2:27028");
  print(JSON.stringify(sh.status()));
'

echo "==> enabling sharding on demo db + collection"
mongosh --quiet --host mongos --port 27020 --eval '
  // Pick any db/coll; here a "products" collection sharded by _id (hashed).
  sh.enableSharding("shop");
  db.getSiblingDB("shop").products.createIndex({ _id: "hashed" });
  sh.shardCollection("shop.products", { _id: "hashed" });
  print("sharding enabled on shop.products");
'

echo "==> done"
