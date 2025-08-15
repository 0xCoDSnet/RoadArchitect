<p align="center">
  <img src="icon.png" width="128" height="128" alt="RoadArchitect icon">
</p>

<p align="center">
  <img alt="Modrinth Downloads" src="https://img.shields.io/modrinth/dt/dLRvLyY3?style=flat&logo=modrinth&link=https%3A%2F%2Fmodrinth.com%2Fmod%2Froadarchitect">
  <img alt="CurseForge Downloads" src="https://img.shields.io/curseforge/dt/1326434?style=flat&logo=curseforge&link=https%3A%2F%2Fwww.curseforge.com%2Fminecraft%2Fmc-mods%2Froadarchitect">
</p>

<p align="center" style="display: flex; justify-content: center; margin: 6px;">
    <a href="https://modrinth.com/mod/roadarchitect/versions?l=fabric">
        <img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/fabric_vector.svg" alt="Available on Fabric">
    </a>
    <a href="https://modrinth.com/mod/roadarchitect/versions?l=quilt">
        <img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/quilt_vector.svg" alt="Available on Quilt">
    </a>
</p>

---
#  RoadArchitect

**RoadArchitect** is a **Fabric** / **Quilt** mod for **Minecraft 1.21.1-1.21.8** that automatically scans the world for villages and other structures, connecting them with roads to form a persistent travel network.  
Roads adapt their style to the biome, and the network is saved between game sessions.

## Features

- 🏘 **Automatic detection** of villages and other structures
- 🎨 **Biome-oriented road styles** for better immersion
- 🧭 **Smart pathfinding** using A* with terrain caching
- 💾 **Persistent network** — roads remain between sessions
- 🔄 **Fully automated**, minimal configuration required

---

## 📷 Screenshots
### Savanna
<p align="left">
  <img src="https://cdn.modrinth.com/data/dLRvLyY3/images/4b3da6120c5ff2e47f76a8c30fc501b09f19fdff.webp" width="512" height="512">
</p>

### Desert
<p align="left">
  <img src="https://cdn.modrinth.com/data/dLRvLyY3/images/295ab7327a26e0178f709c9ea4ea4e884607b5fe.webp" width="512" height="512">
</p>

### Forest
<p align="left">
  <img src="https://cdn.modrinth.com/data/dLRvLyY3/images/827177b77f8dfdbdd7d066a0d5f810645b51f172.webp" width="512" height="512">
</p>

### Taiga
<p align="left">
  <img src="https://cdn.modrinth.com/data/dLRvLyY3/images/138dc531317fc291c44471533066d6d997d53af8.webp" width="512" height="512" alt="RoadArchitect icon">
</p>

### Old growth spruce taiga
<p align="left">
  <img src="https://cdn.modrinth.com/data/dLRvLyY3/images/a228fa4f9649aea42ae66d9269442686df6521a9.webp" width="512" height="512">
</p>

### Plains
<p align="left">
  <img src="https://cdn.modrinth.com/data/dLRvLyY3/images/36c5f56757f8d448dc00cffddbd3ac129cb2bb91.webp" width="512" height="512">
</p>

### River
<p align="left">
  <img src="https://cdn.modrinth.com/data/dLRvLyY3/images/c99e8d83bce571228c71efa6a3f6522551af16d9.webp" width="512" height="512">
</p>

---
## 📥 Installation (for **Minecraft 1.21.1**)
0. Install **[Fabric Loader](https://fabricmc.net/use/installer/)** `0.16.14`
1. Install **[owo-lib](https://modrinth.com/mod/owo-lib)** `0.12.15.4+1.21`
2. Download the mod:
   - From **[Modrinth](https://modrinth.com/mod/roadarchitect)**
   - From **[CurseForge](https://www.curseforge.com/minecraft/mc-mods/roadarchitect)**
3. Place the `.jar` file in your `mods` folder

---

## 📜 Recent changes

<details>
<summary>v1.1.0 — <em>Smoother paths, smarter junctions, cleaner buoys</em></summary>

### ✨ Highlights
- ⚙️ Improved pathfinding (A* / ARA*): adjusted heuristic, removed early termination, expanded profiling.
- 🏗️ Post-processing: added trimming of roads near nodes, improved junction merging and stabilization.
- 🌊 Buoys: now placed only on “clean” water, spaced by real distance, with increased interval (12 → 18).
- 🔧 Fixed client ↔ server sync when registering command arguments.
- 🐛 Fixed swamp style: uses `MOSSY_COBBLESTONE_WALL` instead of `MOSSY_COBBLESTONE`.
- 📦 Reduced mod size.

**⚠ Compatibility:** No breaking changes, worlds from 1.0.1 remain fully compatible.
</details>

---

## 📜 License

This project is licensed under the **Apache License 2.0** — see the [LICENSE](LICENSE) file for details.  
You can also read the full license text here: [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)

---
<p style="text-align: center;">Crafted with ❤️ for the Minecraft community</p>
