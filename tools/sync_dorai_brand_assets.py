# ---------------------------------------------------------------------------
# RETIRED 2026-11-05 -- superseded by the approved Hexa Core (D-shape) identity.
# This script regenerates a pre-Hexa-Core DORAI mark and writes it over active
# app, site, and store assets. It is kept only as project history.
#   Active pipeline : python tools/import_hexacore_assets.py
#   Pack            : docs/branding/dorai/DORAI_HexaCore_Full_LogoPack.zip
# Set DORAI_ALLOW_LEGACY_BRAND_GENERATORS=1 only to reproduce an old concept,
# and only with docs/branding/ output paths -- never against live assets.
# ---------------------------------------------------------------------------
import os as _os, sys as _sys
if _os.environ.get("DORAI_ALLOW_LEGACY_BRAND_GENERATORS") != "1":
    _sys.exit(
        "retired: this script would overwrite the approved Hexa Core logo assets. "
        "Run 'python tools/import_hexacore_assets.py' instead."
    )
# ---------------------------------------------------------------------------

from pathlib import Path
import runpy

root = Path(__file__).resolve().parents[1]
runpy.run_path(str(root / "tools" / "generate_dorai_brand_assets.py"), run_name="__main__")
