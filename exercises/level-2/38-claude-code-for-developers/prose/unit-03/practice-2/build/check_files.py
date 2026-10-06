import json, re, sys, tomllib
from pathlib import Path
def yaml_ok(text):
    for n, line in enumerate(text.splitlines(), 1):
        if re.match(r"^ *\t", line):
            raise ValueError(f"line {n}: a tab in the indentation")
        code = re.sub(r"(\"[^\"]*\"|'[^']*')", "", line.split(" #")[0])
        if code.count("[") != code.count("]") or code.count("{") != code.count("}") or code.count('"') % 2 or code.count("'") % 2:
            raise ValueError(f"line {n}: an unclosed bracket or quote")
root = Path(__file__).resolve().parent
for arg in sys.argv[1:]:
    path = Path(arg).resolve()
    name = path.relative_to(root).as_posix() if root in path.parents else arg
    print(f"== {name}")
    if not path.is_file():
        print("parses: no — the file is missing")
        continue
    text = path.read_text(encoding="utf-8")
    suffix = path.suffix.lower()
    try:
        if suffix == ".json":
            json.loads(text)
        elif suffix == ".toml":
            tomllib.loads(text)
        elif suffix in (".yaml", ".yml"):
            yaml_ok(text)
        elif suffix == ".md" and text.startswith("---"):
            head = text.split("\n---", 1)
            if len(head) < 2:
                raise ValueError("the front matter is not closed with ---")
            yaml_ok(head[0][3:])
        else:
            print(f"{len(text.splitlines())} lines")
            continue
        print("parses: yes")
    except Exception as error:
        print(f"parses: no — {error}")
