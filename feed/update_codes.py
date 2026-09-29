import json, re, time
from pathlib import Path
import requests
from bs4 import BeautifulSoup

SOURCES = {
    "Genshin Impact": [
        ("GIGA", "https://www.giga.de/games/genshin-impact-codes/"),
        ("HoYoLAB", "https://www.hoyolab.com/circles/2/27/official")
    ],
    "Wuthering Waves": [
        ("WuWa UK", "https://wuwa.uk/articles/redeem-codes"),
        ("Pocket Tactics", "https://www.pockettactics.com/wuthering-waves/codes")
    ]
}
# Deliberately conservative: code-like strings only.
PATTERN = re.compile(r"\b[A-Z0-9]{8,16}\b")

def fetch(url):
    r = requests.get(url, timeout=20, headers={"User-Agent":"Mozilla/5.0 GachaCodesBot/1.0"})
    r.raise_for_status()
    return BeautifulSoup(r.text, "html.parser").get_text(" ", strip=True)

def main():
    out = []
    for game, sources in SOURCES.items():
        seen = set()
        for name, url in sources:
            try:
                text = fetch(url)
            except Exception as e:
                print("skip", url, e); continue
            for code in PATTERN.findall(text):
                # Avoid obvious common words / technical strings.
                if code in {"WUTHERINGWAVES","GENSHINIMPACT","POCKETTACTICS","HOYOLAB"}: continue
                if code in seen: continue
                seen.add(code)
                out.append({"game": game, "code": code, "source": name, "seen": time.strftime("%Y-%m-%d %H:%M UTC", time.gmtime())})
    Path("codes.json").write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")

if __name__ == "__main__":
    main()
