# 天道修仙系统 - 命令使用文档

版本: 1.0  
最后更新: 2025-11-07

---

## 📑 目录

1. [玩家命令](#玩家命令)
2. [测试命令](#测试命令)
3. [常见使用场景](#常见使用场景)

---

## 玩家命令

### `/tiandao` (别名: `/cultivation`)

主命令，用于管理修仙系统。

#### 子命令列表

| 命令 | 权限 | 说明 |
|------|------|------|
| `/tiandao` | 所有玩家 | 显示简要帮助信息 |
| `/tiandao help` | 所有玩家 | 显示完整命令列表 |
| `/tiandao status` | 所有玩家 | 查看自己的修仙状态 |
| `/tiandao status <玩家>` | 所有玩家 | 查看其他玩家的修仙状态 |
| `/tiandao setrealm <境界> [等级]` | OP (等级2) | 设置境界 |
| `/tiandao setroot <灵根>` | OP (等级2) | 设置灵根类型 |
| `/tiandao allocate <玩家> [类型] [品质]` | OP (等级2) | 分配灵根 |
| `/tiandao addprogress <数量>` | OP (等级2) | 增加修炼进度 |
| `/tiandao addspiritpower <数量>` | OP (等级2) | 增加灵力 |
| `/tiandao breakthrough` | OP (等级2) | 强制突破境界 |

#### 使用示例

**查看状态**
```
/tiandao status
/tiandao status Steve
```

**设置境界** (OP)
```
/tiandao setrealm qi_condensation
/tiandao setrealm qi_condensation 5
```

可用境界: `mortal`, `qi_condensation`, `foundation_establishment`, `golden_core`, `nascent_soul`, `deity_transformation`

**设置灵根** (OP)
```
/tiandao setroot gold
```

可用灵根: `none`, `gold`, `wood`, `water`, `fire`, `earth`

**分配灵根** (OP)
```
/tiandao allocate Steve                    # 随机分配
/tiandao allocate Steve fire               # 指定类型，随机品质
/tiandao allocate Steve fire excellent     # 指定类型和品质
```

可用品质: `poor`, `normal`, `good`, `excellent`, `heavenly`

**增加修炼进度** (OP)
```
/tiandao addprogress 100
```

**增加灵力** (OP)
```
/tiandao addspiritpower 50
```

**强制突破** (OP)
```
/tiandao breakthrough
```

---

## 测试命令

### `/tiandaotest`

测试命令集合，用于开发和调试。**需要OP权限**。

#### 灵力测试 - `/tiandaotest spirit`

| 命令 | 说明 |
|------|------|
| `/tiandaotest spirit` | 显示当前灵力状态 |
| `/tiandaotest spirit set <数量>` | 设置灵力值 (0-10000) |
| `/tiandaotest spirit recover <秒数>` | 模拟恢复N秒 (1-60) |
| `/tiandaotest spirit info` | 显示详细恢复信息 |

**使用示例:**
```
/tiandaotest spirit
/tiandaotest spirit set 50
/tiandaotest spirit recover 30
/tiandaotest spirit info
```

#### UI测试 - `/tiandaotest ui`

| 命令 | 说明 |
|------|------|
| `/tiandaotest ui` | 显示UI状态 |
| `/tiandaotest ui toggle hud` | 切换HUD显示 |
| `/tiandaotest ui toggle spiritbar` | 切换灵力进度条 |
| `/tiandaotest ui toggle spirittext` | 切换灵力数值显示 |
| `/tiandaotest ui toggle rootinfo` | 切换灵根信息显示 |
| `/tiandaotest ui toggle realminfo` | 切换境界信息显示 |
| `/tiandaotest ui toggle recoveryrate` | 切换恢复速率显示 |
| `/tiandaotest ui position <x> <y>` | 设置HUD位置 |
| `/tiandaotest ui reset` | 重置HUD设置 |

**使用示例:**
```
/tiandaotest ui
/tiandaotest ui toggle hud
/tiandaotest ui position 10 10
/tiandaotest ui reset
```

#### Capability测试 - `/tiandaotest capability`

| 命令 | 说明 |
|------|------|
| `/tiandaotest capability` | 显示Capability信息 |
| `/tiandaotest capability sync` | 强制同步到客户端 |
| `/tiandaotest capability validate` | 验证数据完整性 |

**使用示例:**
```
/tiandaotest capability
/tiandaotest capability sync
/tiandaotest capability validate
```

---

## 常见使用场景

### 场景1: 初始化新玩家

```bash
# 1. 分配随机灵根
/tiandao allocate <玩家名>

# 2. 设置初始境界为炼气期1层
/tiandao setrealm qi_condensation 0

# 3. 给予初始灵力
/tiandao addspiritpower 100
```

### 场景2: 测试灵力恢复系统

```bash
# 1. 设置灵力为较低值
/tiandaotest spirit set 10

# 2. 查看恢复信息
/tiandaotest spirit info

# 3. 模拟恢复30秒
/tiandaotest spirit recover 30

# 4. 查看当前状态
/tiandao status
```

### 场景3: 调试HUD显示

```bash
# 1. 查看当前UI状态
/tiandaotest ui

# 2. 如果HUD不显示，重置设置
/tiandaotest ui reset

# 3. 调整HUD位置
/tiandaotest ui position 15 15

# 4. 切换特定显示元素
/tiandaotest ui toggle spiritbar
```

### 场景4: 测试境界突破

```bash
# 1. 添加足够的修炼进度
/tiandao addprogress 1000

# 2. 查看状态
/tiandao status

# 3. 尝试突破
/tiandao breakthrough
```

### 场景5: 数据同步问题排查

```bash
# 1. 验证Capability数据
/tiandaotest capability validate

# 2. 强制同步到客户端
/tiandaotest capability sync

# 3. 查看同步后的状态
/tiandaotest capability
```

---

## 💡 使用技巧

1. **命令自动补全**: 所有命令都支持Tab键自动补全，包括境界、灵根类型和品质
2. **命令别名**: `/cultivation` 可以替代 `/tiandao`，向后兼容旧命令
3. **权限要求**: 所有测试命令需要OP等级2权限
4. **参数格式**: 境界和灵根类型使用小写加下划线，如 `qi_condensation`, `golden_core`
5. **帮助命令**: 忘记命令时使用 `/tiandao help` 快速查看

---

## ⚠️ 注意事项

- **测试命令仅用于开发**: 不建议在生产服务器上频繁使用测试命令
- **数据同步**: 修改数据后如果HUD未更新，使用 `/tiandaotest capability sync`
- **境界设置**: 直接设置境界会重置修炼进度为0
- **灵力上限**: 设置灵力时会自动限制在最大灵力范围内

---

## 📝 更新日志

### v1.0 (2025-11-07)
- 重构命令系统，统一为 `/tiandao` 和 `/tiandaotest`
- 保留 `/cultivation` 作为别名
- 整合所有测试命令到 `/tiandaotest`
- 添加详细的帮助信息和自动补全
- 移除未使用的物品相关命令

---

## 🔗 相关资源

- 配置文件位置: `config/tiandao-client.toml`, `config/tiandao-common.toml`
- 问题反馈: 请在GitHub Issues提交
- 文档更新: 查看项目Wiki获取最新信息

---

**提示**: 使用命令时遇到问题？尝试 `/tiandao help` 或查看游戏日志文件。

