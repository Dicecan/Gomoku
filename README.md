# Nazuna Gomoku

基于 Java 17 与 HTML5 Canvas 开发的高性能五子棋 / 连珠（Renju）博弈系统。支持禁手裁判、VCF/VCT 算杀求解、主要变例搜索（PVS）以及现代化 Web 对战界面。

---

## 特性概览

- **博弈算法**：
  - 主要变例搜索（PVS）与迭代加深（Iterative Deepening）。
  - 期望窗口（Aspiration Windows）与战术威胁延伸（Threat Extension）。
  - 启发式走法排序：PV 走法、置换表（TT）、杀手走法、对应走法与历史表。
  - 威胁空间算杀引擎：支持连续冲四（VCF）与连续做杀（VCT）求解。
  - 64 位 Zobrist 散列与紧凑无锁置换表（64MB，零 GC 运行）。
- **裁判规则**：
  - 国际连珠联盟（RIF）规则（黑棋三三、四四、长连禁手，递归消解伪活三，成五不判禁）。
  - 自由五子棋规则切换（无禁手限制）。
- **开局体系**：
  - 涵盖 26 种经典连珠开局定式，支持 8 阶二面体对称同构自动匹配。
- **用户界面**：
  - 原生 HTML5 Canvas 绘制木纹棋盘与 3D 光泽棋子。
  - Web Audio API 程序化合成落子与胜利音效。
  - 实时胜率估值条、搜索深度、NPS、已搜节点及主要变例路线（PV Line）显示。
  - 支持无限步数悔棋/重做、SGF 棋谱导入与导出。

---

## 推荐运行配置

- **操作系统**：Windows 10/11、macOS 或 Linux (x86_64 / ARM64)
- **运行环境**：Java 17 或更高版本 (JRE / JDK)
- **硬件配置**：
  - CPU：双核 2.0 GHz 或以上（支持多线程并发搜索）
  - 内存：256 MB 以上可用 RAM
  - 浏览器：Chrome / Edge / Safari / Firefox 最新版本

---

## 项目结构

```
Gomoku/
├── src/main/java/nazuna/gomoku/
│   ├── Main.java                      # 程序入口 (启动服务并唤醒浏览器)
│   ├── core/
│   │   ├── Board.java                 # 棋盘状态、增量 Zobrist、邻域跟踪
│   │   ├── PatternTable.java          # 9 邻域行模式查找表 (3^9=19683)
│   │   ├── RenjuReferee.java          # 连珠裁判 (三三/四四/长连/假活三消除)
│   │   └── Zobrist.java               # 64 位确定性 Zobrist 散列
│   ├── search/
│   │   ├── TranspositionTable.java    # 64 位基元置换表 (64MB)
│   │   ├── MoveList.java              # 走法缓存与局部惰性选择排序
│   │   ├── Evaluator.java             # 局面评估与攻防权值打分
│   │   ├── VCFEngine.java             # 连续冲四算杀求解器
│   │   ├── VCTEngine.java             # 连续做杀算杀求解器
│   │   ├── PVSSearch.java             # PVS 搜索与战术延伸
│   │   ├── OpeningBook.java           # 26 种经典连珠开局库与对称匹配
│   │   ├── GomokuAI.java              # 多线程并行搜索调度器
│   │   └── SearchResult.java          # 搜索结果与胜率映射
│   └── service/
│       ├── GameEngine.java            # 对局状态机、历史栈、SGF 编解码
│       └── GomokuHttpServer.java      # 内置轻量 HTTP 服务与 REST API
├── src/main/resources/web/
│   └── index.html                     # 现代化 Canvas 前端交互界面
├── src/test/java/nazuna/gomoku/       # 单元测试套件
├── build.gradle                       # Gradle 构建文件
├── pom.xml                            # Maven 构建文件
├── run.bat / start.vbs                # Windows 启动脚本
├── run.sh                             # Linux / macOS 启动脚本
└── build.bat                          # 打包脚本
```

---

## 构建与运行

### 1. 快速启动

- **Windows 用户**：双击 `start.vbs`（后台静默启动并自动打开浏览器）或 `run.bat`（带控制台输出）。
- **Linux / macOS 用户**：
  ```bash
  chmod +x run.sh
  ./run.sh
  ```

### 2. 编译打包

使用 Gradle：
```bash
./gradlew jar
```

打包完成后，生成的文件位于 `build/libs/Gomoku-1.0.0.jar`，可使用如下命令运行：
```bash
java -Xms64m -Xmx256m -jar build/libs/Gomoku-1.0.0.jar
```

服务启动后，在浏览器中访问：`http://localhost:8080/` 即可进入对战。

---

## REST API

| 接口 | 方法 | 描述 |
| :--- | :--- | :--- |
| `/` | GET | 返回前端 Web 界面 |
| `/api/state` | GET | 获取当前对局状态与 AI 监控数据 |
| `/api/move` | POST | 玩家落子 `{"index": 112}` 或 `{"x": 7, "y": 7}` |
| `/api/ai-move` | POST | 触发 AI 计算并走步 |
| `/api/new-game` | POST | 开启新对局 `{"humanColor": 1, "ruleMode": 0, "timeLimitSec": 3}` |
| `/api/undo` | POST | 悔棋 `{"steps": 2}` |
| `/api/redo` | POST | 前进 `{"steps": 1}` |
| `/api/sgf/export` | GET | 导出并下载 `.sgf` 棋谱文件 |
| `/api/sgf/import` | POST | 导入 SGF 棋谱文本并恢复局面 |
