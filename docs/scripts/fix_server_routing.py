import subprocess, json, tempfile, os

APIFOX = r"C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd"
CORE = ["92650087", "92650088", "92650089", "92650090",
        "92650091", "92650092", "92650093", "92650094"]
GW = "94571643"
GW_SRV = "bc3ac089-4fe2-441b-9a61-e921625e4e2d"


def apij(cmd):
    p = subprocess.run([APIFOX] + cmd, capture_output=True)
    s = p.stdout.decode("utf-8-sig")
    s = s[s.index("{"): s.rindex("}") + 1]
    return json.loads(s)


def upd(eid, sid):
    f = os.path.join(tempfile.gettempdir(), "upd.json")
    open(f, "w", encoding="utf-8").write(json.dumps({"serverId": sid}))
    p = subprocess.run([APIFOX, "endpoint", "update", str(eid),
                        "--project", "8662286", "--file", f], capture_output=True)
    return p.returncode == 0


total = ok = 0
for fol in CORE + [GW]:
    target = "" if fol in CORE else GW_SRV
    j = apij(["endpoint", "list", "--project", "8662286",
              "--folder-id", fol, "--page-size", "500"])
    for e in j.get("data", []):
        total += 1
        if upd(e["id"], target):
            ok += 1
print("updated %d/%d endpoints (core->default8080, gateway->8000)" % (ok, total))
