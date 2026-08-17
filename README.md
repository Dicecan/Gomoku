<div align="center">

# Nazuna Gomoku Pro

**现代化高强度五子棋 / 连珠（Renju）博弈系统**

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-4EAA25?style=for-the-badge)](https://github.com/Dicecan/Gomoku)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge)](https://github.com/Dicecan/Gomoku)

<p align="center">
  <b>单文件轻量运行 · 零外部依赖 · 经典博弈算法 · 拟真木质 Canvas WebUI</b>
</p>

</div>

---

## 📖 项目简介 (About)

**Nazuna Gomoku** 是一套基于 **Java 17** 与 **原生 HTML5 Canvas** 开发的高性能五子棋 / 连珠对弈系统。

系统采用了现代顶尖棋类 AI 的博弈搜索体系，支持严格的国际连珠联盟（RIF）禁手裁判、VCF/VCT 威胁空间极速算杀、主要变例搜索（PVS）以及多核并行调度。前端界面采用单文件极简设计，程序化生成榧木纹理与 3D 光泽棋子，双击脚本即可在 1 秒内自动拉起浏览器进行对局。

---

## ✨ 核心特性

- 🧠 **博弈与算杀算法**：
  - **主要变例搜索 (PVS)**：配合迭代加深（Iterative Deepening）与期望窗口（Aspiration Windows）动态收窄剪枝。
  - **战术威胁延伸 (Threat Extension)**：在连续叫杀紧迫状态下自动延伸深度，彻底消除地平线盲区。
  - **威胁空间极速求解 (VCF & VCT)**：支持 30+ 步连续冲四杀与做杀推演，毫秒级识破必胜与防守死角。
  - **启发式走法排序**：PV 走法、64MB 无锁紧凑置换表（TT）、杀手走法、对应走法与历史启发。
  - **零 GC 设计**：搜索内循环全静态数组复用，无垃圾回收停顿。
- ⚖️ **国际标准连珠裁判**：
  - 黑棋长连（6+）、四四、三三禁手判定。
  - 递归模拟判定消解**伪活三（假活三）**。
  - 严格遵循**五先原则**（成五不判禁）。
  - 支持自由五子棋（Freestyle）模式无缝切换。
- 📚 **开局定式库**：
  - 内置 26 种经典连珠开局（花月、浦月、溪月、松月、瑞星等）。
  - 采用 **8-阶二面体对称群（Dihedral Symmetry）** 自动映射正规形，0 毫秒出招。
- 🎨 **现代化 Web 对战界面**：
  - 纯原生 Canvas 绘制拟真日本榧木纹理与 3D 黑曜石/珍珠白光泽棋子。
  - 基于 Web Audio API 程序化合成木质落子撞击声与胜利音效，无外部音频文件。
  - 实时胜率仪表盘（Sigmoid 映射）、深度、算力（NPS）、已搜节点数与 PV 路线展示。
  - 支持无限步数悔棋/重做、SGF 标准棋谱导入与导出。

---

## 💻 推荐运行配置

| 规格 | 推荐配置 |
| :--- | :--- |
| **操作系统** | Windows 10 / 11、macOS 11+、Linux (x86_64 / ARM64) |
| **运行环境** | Java Runtime Environment (JRE/JDK) 17 或更高版本 |
| **处理器** | 双核 2.0 GHz 或更高性能 CPU（支持多线程并发搜索） |
| **运行内存** | 256 MB 以上可用 RAM |
| **浏览器** | Google Chrome、Microsoft Edge、Safari、Firefox 等现代浏览器 |

---

## 📁 目录结构

```
Gomoku/
├── src/main/java/nazuna/gomoku/
│   ├── Main.java                      # 服务入口 (启动 HTTP 服务并唤醒系统默认浏览器)
│   ├── core/                          # 核心位棋盘与裁判
│   │   ├── Board.java                 # 15x15 棋盘表示、增量 Zobrist、邻域密度跟踪
│   │   ├── PatternTable.java          # 9 邻域行模式查找表 (3^9=19683 预计算)
│   │   ├── RenjuReferee.java          # 国际标准连珠裁判 (三三/四四/长连/假活三消除)
│   │   └── Zobrist.java               # 64 位确定性 Zobrist 散列
│   ├── search/                        # 核心博弈引擎
│   │   ├── TranspositionTable.java    # 64 位基元无锁置换表 (64MB)
│   │   ├── MoveList.java              # 走法缓存与局部惰性选择排序
│   │   ├── Evaluator.java             # 局面评估与攻防紧迫度打分
│   │   ├── VCFEngine.java             # 连续冲四 (VCF) 绝杀求解器
│   │   ├── VCTEngine.java             # 连续做杀 (VCT) 绝杀求解器
│   │   ├── PVSSearch.java             # PVS 搜索 + 期望窗口 + 战术延伸
│   │   ├── OpeningBook.java           # 26 种经典连珠开局库与 8 阶对称匹配
│   │   ├── GomokuAI.java              # 多线程 Lazy SMP 调度器
│   │   └── SearchResult.java          # 搜索元数据与胜率映射
│   └── service/                       # 业务与服务层
│       ├── GameEngine.java            # 状态机管理、历史栈、SGF 编解码
│       └── GomokuHttpServer.java      # 内置轻量 HTTP 服务与 REST API
├── src/main/resources/web/
│   └── index.html                     # 纯原生 Canvas 现代化前端单页面
├── src/test/java/nazuna/gomoku/       # 规则与算法单元测试套件
├── build.gradle / pom.xml             # Gradle / Maven 项目配置
├── run.bat / start.vbs                # Windows 启动脚本 (支持无黑框静默运行)
├── run.sh                             # Linux / macOS 启动脚本
└── build.bat                          # 一键编译打包脚本
```

---

## 🚀 快速启动

### 方式一：Windows 用户（极简双击）
- **推荐**：双击 **`start.vbs`**（静默后台启动，**无任何黑框命令行闪烁**，自动打开浏览器对战）。
- **调试**：双击 **`run.bat`**（附带终端实时状态与监控输出）。

### 方式二：Linux / macOS 用户
```bash
chmod +x run.sh
./run.sh
```

### 方式三：手动命令行运行
```bash
# 1. 编译并打包为可执行 Fat JAR
./gradlew jar

# 2. 启动服务
java -Xms64m -Xmx256m -jar build/libs/Gomoku-1.0.0.jar
```

启动后在浏览器打开 **`http://localhost:8080/`** 即可开始对弈。

---

## 📡 REST API 说明

系统内置了标准轻量 REST 接口，支持二次开发或接入自定义客户端：

| 端点 | 请求方式 | 参数说明 | 接口功能 |
| :--- | :--- | :--- | :--- |
| `/` | `GET` | 无 | 返回 Canvas WebUI 前端页面 |
| `/api/state` | `GET` | 无 | 获取当前完整棋局状态与 AI 监控指标 |
| `/api/move` | `POST` | `{"index": 112}` 或 `{"x": 7, "y": 7}` | 玩家落子并更新胜负判定 |
| `/api/ai-move` | `POST` | 无 | 触发 AI 计算并走步 |
| `/api/new-game` | `POST` | `{"humanColor": 1, "ruleMode": 0, "timeLimitSec": 3}` | 开新局（设置先手/规则/算力时间） |
| `/api/undo` | `POST` | `{"steps": 2}` | 悔棋操作 |
| `/api/redo` | `POST` | `{"steps": 1}` | 前进 / 重做一步 |
| `/api/sgf/export` | `GET` | 无 | 导出并下载当前对局 SGF 棋谱 |
| `/api/sgf/import` | `POST` | SGF 字符串内容 | 导入 SGF 棋谱并恢复局面 |

---

## 📄 开源协议

本项目基于 [GNU General Public License v3.0 (GPLv3)](LICENSE) 协议开源。
