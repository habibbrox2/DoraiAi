from pathlib import Path
import runpy

root = Path(__file__).resolve().parents[1]
runpy.run_path(str(root / "tools" / "generate_dorai_brand_assets.py"), run_name="__main__")
