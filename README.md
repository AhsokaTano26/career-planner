辅导员端：
建立数据库MySQL并删除临时数据库
实现辅导员端通用层服务
构建adivisor模块骨架
实现查询层与服务
指导意见/建议服务
已实现Controller 层已实现
单元测试通过

问题：AI组需要完成
依赖表（assessment/snapshot/recommendation/goal/plan/task/review/direction）字段以设计文档为准，已按最小结构在 dev 脚本中给出样例；正式建表以各模块队友 schema 为准；
审计（详情查看写日志）未实现，代码中未留调用点，后续接入 AuditLogWriter 即可；
POST /advice 与 /guidance 共用同一写入逻辑（仅路径不同），如产品上要限制 /advice 仅允许 SUGGEST_* 可再加校验；
提交历史含环境自动提交（信息较随意），合并 PR 时如需要可 squash；

管理端：用户与白名单
用户列表/更新、关系列表/批量建立/解除、白名单列表/新增/CSV 导入/删除；

管理端：配置
