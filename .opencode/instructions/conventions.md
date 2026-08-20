# 开发约定

以下约定在每次回答中必须遵守：

- **接口与鉴权**：统一前缀 `/api/v1`；JWT 认证（Spring Security + JJWT），登录接口 `POST /api/v1/auth/login` 获取 token，其余接口带 `Authorization: Bearer <token>`；涉及接口改动前先对照 `docs/接口设计.md` 与 `docs/openapi/career-core-apis.yaml`。
- **分层结构**：后端在 `career-core` 的 `com.rickgao.careercore.modules.<模块>` 下，每个模块 = controller + dto + entity + mapper + service(+impl) + vo；Mapper 接口 + XML 在 `resources/mapper/<模块>/`；公共类在 `com.rickgao.careercore.common`、`config`、`security`。
- **数据库约定**：表/字段 snake_case、主键 varchar(32)（IdGenerator 生成，非自增 bigint）、通用列 `created_at/updated_at`；**优先沿用现有表结构，不轻易新增表/字段**；MySQL 保留字（如 `rank`）需反引号。
- **注释约定**：所有 Demo 简化/增强/兼容逻辑处必须用注释标明「Demo 精简点 / 后续迭代替换位置」，便于后续识别替换。
- **语言与编码**：代码与数据统一 UTF-8；与用户使用中文交流。
- **版本对齐**：修改任何依赖（pom.xml / package.json / requirements.txt）前先对照「本机 vs Docker 对齐表」（见 `project-environment.md` 第 3 节），确保本地与 Docker 一致；修改后必须同步更新 `project-environment.md`。