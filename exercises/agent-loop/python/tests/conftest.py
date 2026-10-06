import os
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(HERE.parent / "starter")))
