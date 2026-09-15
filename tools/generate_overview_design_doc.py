from __future__ import annotations

import zipfile
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable
from xml.sax.saxutils import escape


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "校园二手交易系统概要设计说明书.docx"


def xml_escape(text: str) -> str:
    return escape(text, {'"': "&quot;", "'": "&apos;"})


def run_xml(text: str, *, bold: bool = False, font_size: int | None = None) -> str:
    props = []
    if bold:
        props.append("<w:b/><w:bCs/>")
    if font_size is not None:
        half_points = font_size * 2
        props.append(f"<w:sz w:val=\"{half_points}\"/><w:szCs w:val=\"{half_points}\"/>")
    rpr = f"<w:rPr>{''.join(props)}</w:rPr>" if props else ""
    preserve = ' xml:space="preserve"' if text[:1].isspace() or text[-1:].isspace() else ""
    return f"<w:r>{rpr}<w:t{preserve}>{xml_escape(text)}</w:t></w:r>"


def paragraph_xml(
    text: str,
    *,
    style: str | None = None,
    align: str | None = None,
    bold: bool = False,
    font_size: int | None = None,
    page_break_before: bool = False,
) -> str:
    ppr = []
    if style:
        ppr.append(f"<w:pStyle w:val=\"{style}\"/>")
    if align:
        ppr.append(f"<w:jc w:val=\"{align}\"/>")
    if page_break_before:
        ppr.append("<w:pageBreakBefore/>")
    ppr_xml = f"<w:pPr>{''.join(ppr)}</w:pPr>" if ppr else ""
    return f"<w:p>{ppr_xml}{run_xml(text, bold=bold, font_size=font_size)}</w:p>"


def blank_paragraph() -> str:
    return "<w:p/>"


def page_break_xml() -> str:
    return "<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>"


def table_xml(rows: list[list[str]], widths: list[int], *, header: bool = True) -> str:
    total_width = sum(widths)
    grid = "".join(f"<w:gridCol w:w=\"{w}\"/>" for w in widths)
    row_xml = []
    border = (
        "<w:tblBorders>"
        "<w:top w:val=\"single\" w:sz=\"8\" w:space=\"0\" w:color=\"B7C3D0\"/>"
        "<w:left w:val=\"single\" w:sz=\"8\" w:space=\"0\" w:color=\"B7C3D0\"/>"
        "<w:bottom w:val=\"single\" w:sz=\"8\" w:space=\"0\" w:color=\"B7C3D0\"/>"
        "<w:right w:val=\"single\" w:sz=\"8\" w:space=\"0\" w:color=\"B7C3D0\"/>"
        "<w:insideH w:val=\"single\" w:sz=\"8\" w:space=\"0\" w:color=\"D5DDE5\"/>"
        "<w:insideV w:val=\"single\" w:sz=\"8\" w:space=\"0\" w:color=\"D5DDE5\"/>"
        "</w:tblBorders>"
    )
    for row_index, row in enumerate(rows):
        cells = []
        for col_index, cell in enumerate(row):
            shading = ""
            if header and row_index == 0:
                shading = "<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"DCE6F1\"/>"
            tc_pr = (
                f"<w:tcPr><w:tcW w:w=\"{widths[col_index]}\" w:type=\"dxa\"/>"
                f"{shading}"
                "<w:vAlign w:val=\"center\"/>"
                "</w:tcPr>"
            )
            content = paragraph_xml(cell, bold=header and row_index == 0)
            cells.append(f"<w:tc>{tc_pr}{content}</w:tc>")
        row_xml.append(f"<w:tr>{''.join(cells)}</w:tr>")
    return (
        "<w:tbl>"
        f"<w:tblPr><w:tblW w:w=\"{total_width}\" w:type=\"dxa\"/>{border}"
        "<w:tblCellMar><w:top w:w=\"90\" w:type=\"dxa\"/><w:left w:w=\"120\" w:type=\"dxa\"/>"
        "<w:bottom w:w=\"90\" w:type=\"dxa\"/><w:right w:w=\"120\" w:type=\"dxa\"/></w:tblCellMar>"
        "</w:tblPr>"
        f"<w:tblGrid>{grid}</w:tblGrid>"
        f"{''.join(row_xml)}"
        "</w:tbl>"
    )


@dataclass
class DocumentBuilder:
    parts: list[str]

    def add_paragraph(
        self,
        text: str,
        *,
        style: str | None = None,
        align: str | None = None,
        bold: bool = False,
        font_size: int | None = None,
        page_break_before: bool = False,
    ) -> None:
        self.parts.append(
            paragraph_xml(
                text,
                style=style,
                align=align,
                bold=bold,
                font_size=font_size,
                page_break_before=page_break_before,
            )
        )

    def add_lines(self, lines: Iterable[str], *, style: str | None = None, align: str | None = None) -> None:
        for line in lines:
            self.add_paragraph(line, style=style, align=align)

    def add_table(self, rows: list[list[str]], widths: list[int], *, header: bool = True) -> None:
        self.parts.append(table_xml(rows, widths, header=header))

    def add_blank(self) -> None:
        self.parts.append(blank_paragraph())

    def add_page_break(self) -> None:
        self.parts.append(page_break_xml())


def build_content() -> str:
    doc = DocumentBuilder(parts=[])

    doc.add_blank()
    doc.add_blank()
    doc.add_blank()
    doc.add_paragraph("校园二手交易系统", style="Title", align="center")
    doc.add_paragraph("概要设计说明书", style="Title", align="center")
    doc.add_blank()
    doc.add_blank()
    doc.add_paragraph("项目类型：B/S 架构 Web 应用", align="center")
    doc.add_paragraph("技术体系：Vue 3 + Spring Boot 3 + MyBatis-Plus + MySQL", align="center")
    doc.add_paragraph("文档版本：V1.0", align="center")
    doc.add_paragraph("编制日期：2026年04月16日", align="center")
    doc.add_page_break()

    doc.add_paragraph("修订记录", style="Heading1")
    doc.add_table(
        [
            ["版本", "日期", "修订说明", "编制人"],
            ["V1.0", "2026-04-16", "首次根据项目现状形成概要设计说明书", "Codex"],
        ],
        [1100, 1800, 4800, 1300],
    )
    doc.add_blank()

    doc.add_paragraph("目录", style="Heading1")
    doc.add_lines(
        [
            "1 引言",
            "1.1 编写目的",
            "1.2 项目背景",
            "1.3 术语与缩略语",
            "1.4 参考资料",
            "2 总体设计",
            "2.1 需求规定",
            "2.2 运行环境",
            "2.3 基本设计概念和处理流程",
            "2.4 系统结构设计",
            "2.5 功能模块与程序模块关系",
            "3 接口设计",
            "3.1 用户接口",
            "3.2 外部接口",
            "3.3 内部接口",
            "4 运行设计",
            "4.1 运行模块组合",
            "4.2 运行控制",
            "4.3 运行时间与性能考虑",
            "5 系统数据结构设计",
            "5.1 逻辑结构设计要点",
            "5.2 物理结构设计要点",
            "5.3 数据结构与程序关系",
            "6 系统出错处理设计",
            "6.1 出错信息设计",
            "6.2 补救措施",
            "6.3 系统维护设计",
            "7 安全与部署设计",
            "7.1 安全设计",
            "7.2 部署设计",
            "7.3 配置与运维建议",
            "8 附录",
            "8.1 核心数据表清单",
            "8.2 主要接口清单",
        ]
    )
    doc.add_page_break()

    doc.add_paragraph("1 引言", style="Heading1")
    doc.add_paragraph("1.1 编写目的", style="Heading2")
    doc.add_paragraph(
        "本文档用于说明校园二手交易系统的总体技术方案、模块划分、接口边界、数据结构、运行机制和部署方式，"
        "为后续详细设计、编码实现、测试验收和系统维护提供统一依据。"
    )
    doc.add_paragraph("1.2 项目背景", style="Heading2")
    doc.add_paragraph(
        "本项目面向校园场景下的闲置物品交易、求购、公告通知和后台治理需求，采用前后端分离的 B/S 架构实现。"
        "系统覆盖游客浏览、学生注册审核、商品发布与交易、评论互动、消息通知、推荐、报表导出及演示数据初始化等能力。"
    )
    doc.add_paragraph("1.3 术语与缩略语", style="Heading2")
    doc.add_table(
        [
            ["术语", "说明"],
            ["B/S", "Browser/Server，浏览器/服务器架构。"],
            ["JWT", "JSON Web Token，用于无状态身份认证。"],
            ["RBAC", "基于角色的访问控制，本系统对应超级管理员、审核员、操作员和普通用户。"],
            ["OLTP", "联机事务处理，强调高并发读写和事务一致性。"],
            ["OpenAPI", "接口描述标准，用于生成 Swagger 在线接口文档。"],
        ],
        [1800, 7200],
    )
    doc.add_paragraph("1.4 参考资料", style="Heading2")
    doc.add_lines(
        [
            "（1）项目源码与配置：Spring Boot 后端、Vue 3 前端、数据库脚本与测试代码。",
            "（2）数据库设计说明：database/docs/SCHEMA.md、database/docs/ERD.md。",
            "（3）接口说明：README.md 中列出的核心 API 与 Swagger/OpenAPI 配置。",
            "（4）项目依赖与约束：pom.xml、frontend/package.json、application.yml。",
        ]
    )

    doc.add_paragraph("2 总体设计", style="Heading1")
    doc.add_paragraph("2.1 需求规定", style="Heading2")
    doc.add_paragraph(
        "系统需要支持三类访问角色：游客、注册用户、后台管理员。游客可浏览商品、求购和公告并提交注册申请；"
        "注册用户可登录后维护个人资料、发布商品、发表评论、提交求购、下单交易、查看通知与推荐；"
        "后台管理员可登录后台执行注册审核、用户治理、商品治理、订单治理、公告发布、SMTP 配置、统计报表导出和演示数据管理。"
    )
    doc.add_table(
        [
            ["角色", "核心职责", "典型页面/接口"],
            ["游客", "浏览公开信息、上传学生证、提交注册申请", "首页、商品列表、求购列表、公告页、注册页"],
            ["用户", "发布商品、管理订单、管理求购、查看消息和推荐", "个人中心、发布页、订单页、通知页、推荐页"],
            ["管理员", "审核注册、管理用户/商品/订单/公告、查看报表", "后台仪表盘、审核页、用户页、订单页、报表页"],
        ],
        [1100, 3400, 4500],
    )
    doc.add_paragraph("2.2 运行环境", style="Heading2")
    doc.add_table(
        [
            ["层次", "运行环境", "说明"],
            ["前端", "Vue 3 + Vite + TypeScript", "浏览器访问，路由按公共端、用户端、管理端组织。"],
            ["后端", "Spring Boot 3.3 + Java 17", "提供 REST API、认证鉴权、业务处理和报表导出。"],
            ["数据层", "MySQL 8.0 + InnoDB", "存储用户、商品、订单、行为日志、推荐结果等核心业务数据。"],
            ["文件层", "本地存储目录 storage", "保存学生证、头像、商品图片等媒体文件，并通过 /uploads 对外访问。"],
            ["通知层", "SMTP 邮件服务（可选）", "用于注册审核通知等邮件发送，站内通知始终可用。"],
        ],
        [1200, 2500, 4300],
    )
    doc.add_paragraph("2.3 基本设计概念和处理流程", style="Heading2")
    doc.add_paragraph(
        "系统采用前后端分离和无状态认证设计。前端通过 Axios 调用后端 REST 接口；后端使用 Spring Security + JWT 实现身份校验，"
        "通过 Controller、Service、Mapper 三层结构组织业务逻辑和数据访问；MySQL 负责业务持久化，本地文件存储负责图片与文档资源。"
    )
    doc.add_lines(
        [
            "总体处理链路如下：",
            "浏览器 / 管理端页面  ->  Vue Router 页面组件  ->  Axios HTTP 请求",
            "->  Spring Boot Controller  ->  Service 业务层  ->  MyBatis-Plus Mapper",
            "->  MySQL 数据库 / 本地文件存储 / SMTP 服务",
            "->  ApiResponse JSON  ->  前端页面渲染与状态更新",
        ]
    )
    doc.add_paragraph(
        "对于推荐场景，系统额外采集搜索历史、浏览/评论/发布/购买等行为日志，结合求购信息生成推荐得分，"
        "并将结果写入 user_recommendations 表供前端快速读取。"
    )
    doc.add_paragraph("2.4 系统结构设计", style="Heading2")
    doc.add_table(
        [
            ["结构层次", "主要组成", "职责"],
            ["表示层", "Vue 页面、布局、组件、Pinia 状态", "负责用户交互、页面路由、表单校验和图表展示。"],
            ["接口层", "Public/User/Admin Controllers", "按访问身份暴露 REST 接口，并完成参数接收与权限边界控制。"],
            ["业务层", "各类 Service / ServiceImpl", "实现注册审核、商品管理、订单流转、通知、推荐、报表等核心业务规则。"],
            ["持久层", "Mapper + Entity + MyBatis-Plus", "封装 CRUD、分页、条件查询和事务性数据更新。"],
            ["基础能力层", "JWT、安全配置、OpenAPI、存储、SMTP", "提供鉴权、接口文档、文件管理、邮件发送等通用能力。"],
        ],
        [1300, 3000, 4200],
    )
    doc.add_paragraph("2.5 功能模块与程序模块关系", style="Heading2")
    doc.add_table(
        [
            ["功能域", "后端模块", "前端模块"],
            ["注册与登录", "PublicRegistration、UserAuth、AdminAuth、LoginCaptcha", "注册页、用户登录页、管理员登录页"],
            ["商品与分类", "PublicItem、UserItem、ItemCategory、FileStorage", "首页、商品列表、商品详情、发布页、我的商品"],
            ["评论与互动", "PublicItemComment、UserItemComment、Notification", "商品详情评论区、收到的评论"],
            ["求购", "PublicWantedPost、UserWantedPost", "求购列表、求购详情、我的求购"],
            ["订单交易", "UserOrder、AdminOrderManagement", "用户订单页、后台订单页"],
            ["通知与公告", "PublicAnnouncement、AdminAnnouncement、UserNotificationCenter、Notification", "公告页、通知页、后台公告页"],
            ["推荐", "RecommendationBehavior、UserRecommendation", "个性化推荐页、首页推荐数据"],
            ["后台治理", "AdminUserManagement、AdminDashboard、AdminReportExport、AdminDemoMode、SmtpSettings", "后台仪表盘、用户/商品/订单/报表/邮件配置页面"],
        ],
        [1600, 3400, 3500],
    )

    doc.add_paragraph("3 接口设计", style="Heading1")
    doc.add_paragraph("3.1 用户接口", style="Heading2")
    doc.add_paragraph("前端路由分为公共端、用户端和管理端三组，采用 Vue Router 和路由守卫控制访问。")
    doc.add_table(
        [
            ["接口类别", "典型页面/路由", "说明"],
            ["公共端", "/、/items、/wanted-posts、/announcements、/register", "无需登录即可访问，提供公开浏览和注册入口。"],
            ["用户端", "/user/profile、/user/publish、/user/orders、/user/notifications、/user/recommendations", "要求普通用户登录后访问。"],
            ["管理端", "/admin/dashboard、/admin/registrations、/admin/users、/admin/orders、/admin/reports", "要求管理员登录并具备对应角色权限。"],
        ],
        [1400, 3600, 3500],
    )
    doc.add_paragraph("3.2 外部接口", style="Heading2")
    doc.add_table(
        [
            ["接口对象", "交互方式", "设计说明"],
            ["浏览器前端", "HTTP/JSON", "统一以 /api/v1/** 提供 REST 服务，返回 ApiResponse 封装结果。"],
            ["MySQL", "JDBC", "通过 MyBatis-Plus 完成实体映射、分页查询和事务更新。"],
            ["文件存储", "Multipart 上传 + 静态资源访问", "学生证、头像、商品图片上传至本地目录，并映射为 /uploads/**。"],
            ["SMTP 服务", "SMTP 协议", "用于发送注册审核等邮件通知，未配置时邮件通知记录为待发送或失败。"],
            ["Swagger/OpenAPI", "HTTP 页面与 JSON 文档", "提供 swagger-ui.html 和 /v3/api-docs 便于调试与联调。"],
        ],
        [1400, 2200, 4700],
    )
    doc.add_paragraph("3.3 内部接口", style="Heading2")
    doc.add_paragraph(
        "后端内部接口采用分层调用方式。Controller 只负责鉴权入口、参数绑定和响应包装，"
        "Service 承担业务规则、事务与流程编排，Mapper 负责数据库持久化。"
    )
    doc.add_lines(
        [
            "典型内部调用关系如下：",
            "（1）注册审核：AdminRegistrationApplicationController -> RegistrationReviewService -> RegistrationApplicationMapper / UserMapper / NotificationService。",
            "（2）商品发布：UserItemController -> UserItemService -> ItemMapper / ItemImageMapper / MediaFileMapper。",
            "（3）订单流转：UserOrderController -> UserOrderService -> TradeOrderMapper / OrderItemMapper / OrderStatusLogMapper。",
            "（4）推荐刷新：UserRecommendationController -> UserRecommendationService -> UserBehaviorLogMapper / SearchHistoryMapper / UserRecommendationMapper。",
            "（5）后台报表：AdminReportExportController -> AdminReportExportService -> 多表聚合查询并导出 CSV。",
        ]
    )

    doc.add_paragraph("4 运行设计", style="Heading1")
    doc.add_paragraph("4.1 运行模块组合", style="Heading2")
    doc.add_paragraph(
        "系统运行时由前端单页应用、后端应用服务、数据库和文件存储共同组成。前端通过浏览器加载后进行页面内路由切换；"
        "后端单体应用同时承担公开接口、用户接口和管理接口；数据库保存交易数据、配置数据和日志数据。"
    )
    doc.add_paragraph("4.2 运行控制", style="Heading2")
    doc.add_lines(
        [
            "主要业务流程控制如下：",
            "（1）注册审核流程：游客上传学生证并提交注册申请 -> 管理员审核 -> 审核通过后生成用户账号并发送站内/邮件通知。",
            "（2）商品发布流程：用户上传商品图片 -> 填写商品信息 -> 保存商品与图片关联 -> 公开列表可检索与浏览。",
            "（3）订单交易流程：买家提交订单 -> 卖家确认 -> 配送/面交 -> 买家确认完成；过程中写入状态日志。",
            "（4）推荐流程：系统记录用户搜索、浏览、评论、发布、购买、求购行为 -> 计算分类分、关键词分、热度分 -> 生成推荐结果。",
            "（5）后台治理流程：管理员依据角色权限管理用户、商品、订单、公告和报表，并记录管理员操作日志。",
        ]
    )
    doc.add_paragraph("4.3 运行时间与性能考虑", style="Heading2")
    doc.add_lines(
        [
            "（1）查询接口统一使用分页结构，避免一次性加载过多记录。",
            "（2）商品与求购检索使用全文索引和组合索引提升筛选效率。",
            "（3）推荐结果采用预计算落表方式，减少首页和个人中心实时计算压力。",
            "（4）订单状态日志、通知记录、管理员操作日志分表存储，降低核心交易表写入耦合度。",
            "（5）前端采用按路由懒加载页面组件，减少首屏资源体积。",
        ]
    )

    doc.add_paragraph("5 系统数据结构设计", style="Heading1")
    doc.add_paragraph("5.1 逻辑结构设计要点", style="Heading2")
    doc.add_paragraph(
        "数据库按业务域划分为账号域、商品域、交易域、推荐分析域和治理域。账号域负责用户、管理员和注册申请；"
        "商品域负责分类、商品、图片、评论和求购；交易域负责订单、订单项和状态日志；"
        "推荐分析域负责搜索记录、行为日志和推荐结果；治理域负责公告、通知、系统设置和管理员操作日志。"
    )
    doc.add_table(
        [
            ["数据域", "核心表", "设计要点"],
            ["账号域", "admins、registration_applications、users、login_logs", "区分申请态与正式账号态，支持审核追踪与登录审计。"],
            ["商品域", "item_categories、items、item_images、item_comments、wanted_posts、media_files", "支持多图、层级分类、评论回复、求购与文件元数据管理。"],
            ["交易域", "orders、order_items、order_status_logs", "采用订单快照与状态日志，保证交易历史可追溯。"],
            ["推荐分析域", "search_histories、user_behavior_logs、user_recommendations", "采集行为特征并缓存推荐结果。"],
            ["治理域", "announcements、notifications、admin_operation_logs、system_settings", "支持公告发布、通知投递、演示模式和操作审计。"],
        ],
        [1200, 3600, 3800],
    )
    doc.add_paragraph("5.2 物理结构设计要点", style="Heading2")
    doc.add_lines(
        [
            "（1）数据库采用 MySQL 8.0、utf8mb4 字符集和 InnoDB 引擎。",
            "（2）对商品、订单、注册申请、通知等高频查询场景设置组合索引。",
            "（3）items 与 wanted_posts 设置全文索引，以支持标题、品牌、型号和描述的检索。",
            "（4）users、items、wanted_posts、item_comments 等用户面向数据表保留 deleted_at 字段，支持软删除。",
            "（5）order_items 保存商品标题和价格快照，避免商品后续修改影响历史订单。",
            "（6）user_recommendations 设置生成时间和过期时间，便于推荐结果失效与刷新。",
        ]
    )
    doc.add_paragraph("5.3 数据结构与程序关系", style="Heading2")
    doc.add_table(
        [
            ["程序模块", "主要实体/表", "关系说明"],
            ["注册审核服务", "RegistrationApplication、User、Notification", "审核通过后由申请表转换为用户表记录，并生成通知。"],
            ["商品管理服务", "Item、ItemImage、MediaFile、ItemCategory", "商品与图片为一对多关系，图片文件由媒体表统一管理。"],
            ["评论服务", "ItemComment、Notification", "评论和回复写入评论表，必要时触发通知。"],
            ["订单服务", "TradeOrder、OrderItem、OrderStatusLog", "订单主表负责状态，明细表负责快照，日志表记录流转。"],
            ["推荐服务", "SearchHistory、UserBehaviorLog、UserRecommendation、Item", "行为与搜索为输入，推荐结果表为输出。"],
            ["后台治理服务", "Announcement、AdminOperationLog、SystemSetting", "后台操作既更新业务数据，也写入设置与审计日志。"],
        ],
        [1500, 3300, 3800],
    )

    doc.add_paragraph("6 系统出错处理设计", style="Heading1")
    doc.add_paragraph("6.1 出错信息设计", style="Heading2")
    doc.add_paragraph(
        "系统统一以 ApiResponse 返回业务结果。认证失败返回 40100，鉴权失败返回 40300，"
        "业务校验失败由 BusinessException 提供明确错误码和错误信息，便于前端提示与定位。"
    )
    doc.add_table(
        [
            ["错误类别", "触发场景", "处理方式"],
            ["认证错误", "未登录、JWT 无效、令牌过期", "返回 401 状态和统一 JSON 错误体。"],
            ["授权错误", "角色权限不足访问后台接口", "返回 403 状态和统一 JSON 错误体。"],
            ["业务错误", "订单不存在、推荐不存在、审核状态非法等", "抛出 BusinessException，返回业务错误码。"],
            ["校验错误", "参数缺失、格式非法、价格区间错误", "由参数校验或数据库约束拦截。"],
            ["外部服务错误", "SMTP 发送失败、文件写入失败", "记录失败状态或抛出服务异常，避免脏数据提交。"],
        ],
        [1400, 3400, 3300],
    )
    doc.add_paragraph("6.2 补救措施", style="Heading2")
    doc.add_lines(
        [
            "（1）关键业务使用事务控制，确保订单、审核、推荐刷新等流程的原子性。",
            "（2）通知发送失败时保留通知记录并标记状态，便于后续补发或人工排查。",
            "（3）管理员治理动作写入 admin_operation_logs，便于事后追溯。",
            "（4）登录行为写入 login_logs，便于排查验证码、密码错误和异常访问问题。",
            "（5）文件通过 media_files 记录元数据，发生文件问题时可定位来源、路径和校验信息。",
        ]
    )
    doc.add_paragraph("6.3 系统维护设计", style="Heading2")
    doc.add_lines(
        [
            "（1）通过 Swagger/OpenAPI 暴露接口文档，降低联调和维护成本。",
            "（2）通过 system_settings 保存演示模式和运行时配置，避免硬编码开关。",
            "（3）通过后台报表导出 CSV，支持运营分析和离线审查。",
            "（4）项目已包含较完整的 Service/Controller 测试，为后续回归验证提供基础。",
            "（5）管理员邮件设置、演示数据和报告导出均作为独立服务模块，便于演进和替换。",
        ]
    )

    doc.add_paragraph("7 安全与部署设计", style="Heading1")
    doc.add_paragraph("7.1 安全设计", style="Heading2")
    doc.add_lines(
        [
            "（1）采用 Spring Security + JWT 实现无状态认证，管理员与普通用户路径分离。",
            "（2）采用 BCryptPasswordEncoder 保存口令哈希，数据库中不存储明文密码。",
            "（3）后台接口按角色划分访问权限：SUPER_ADMIN、AUDITOR、OPERATOR。",
            "（4）公开接口、登录接口、Swagger 文档和上传资源路径单独放行，其余接口默认要求认证。",
            "（5）登录流程引入验证码，降低暴力破解和脚本攻击风险。",
            "（6）核心业务表保留审计字段与日志表，满足溯源和风控需要。",
        ]
    )
    doc.add_paragraph("7.2 部署设计", style="Heading2")
    doc.add_lines(
        [
            "推荐部署形态如下：",
            "（1）Nginx / 静态 Web 服务：部署前端构建产物 frontend/dist。",
            "（2）Spring Boot 应用节点：部署后端服务，对外提供 8080 端口 REST API。",
            "（3）MySQL 节点：存储业务数据与运行配置。",
            "（4）文件存储目录：与应用节点同机或外挂存储卷，保存上传资源。",
            "（5）SMTP 服务：作为外部邮件发送依赖按需接入。",
        ]
    )
    doc.add_paragraph(
        "若采用开发环境，Vite 可通过代理将 /api 与 /uploads 转发至后端；生产环境建议由 Nginx 统一反向代理前端静态资源、后端接口和上传资源。"
    )
    doc.add_paragraph("7.3 配置与运维建议", style="Heading2")
    doc.add_table(
        [
            ["配置项", "用途", "说明"],
            ["DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD", "数据库连接", "用于后端连接 MySQL。"],
            ["JWT_SECRET", "令牌签名", "建议在生产环境中单独保管并定期轮换。"],
            ["STORAGE_ROOT_DIR", "文件存储根目录", "建议配置到独立持久化卷。"],
            ["STORAGE_PUBLIC_BASE_URL", "上传资源访问前缀", "用于构造文件公开访问 URL。"],
            ["spring.mail.*", "邮件服务配置", "按需启用邮件通知功能。"],
        ],
        [2500, 2200, 3300],
    )

    doc.add_paragraph("8 附录", style="Heading1")
    doc.add_paragraph("8.1 核心数据表清单", style="Heading2")
    doc.add_table(
        [
            ["序号", "表名", "用途"],
            ["1", "admins", "管理员账号"],
            ["2", "registration_applications", "注册申请与审核跟踪"],
            ["3", "users", "正式用户账号"],
            ["4", "login_logs", "登录审计日志"],
            ["5", "media_files", "上传文件元数据"],
            ["6", "item_categories", "分类树"],
            ["7", "items", "商品主表"],
            ["8", "item_images", "商品图片关联"],
            ["9", "item_comments", "商品评论与回复"],
            ["10", "wanted_posts", "求购信息"],
            ["11", "orders", "订单主表"],
            ["12", "order_items", "订单商品快照"],
            ["13", "order_status_logs", "订单状态变更日志"],
            ["14", "search_histories", "搜索历史"],
            ["15", "user_behavior_logs", "用户行为日志"],
            ["16", "user_recommendations", "推荐结果"],
            ["17", "announcements", "公告"],
            ["18", "notifications", "站内/邮件通知"],
            ["19", "admin_operation_logs", "管理员操作日志"],
            ["20", "system_settings", "系统运行配置"],
        ],
        [900, 3000, 5100],
    )
    doc.add_paragraph("8.2 主要接口清单", style="Heading2")
    doc.add_table(
        [
            ["接口分组", "代表接口", "用途"],
            ["公共接口", "/api/v1/public/items、/wanted-posts、/announcements、/registration-applications", "游客浏览与注册申请"],
            ["用户接口", "/api/v1/user/profile、/items、/orders、/notifications、/recommendations", "用户业务办理"],
            ["管理员接口", "/api/v1/admin/registration-applications、/users、/items、/orders、/reports", "后台审核治理与报表导出"],
            ["认证接口", "/api/v1/user/auth/*、/api/v1/admin/auth/*", "验证码获取、登录与身份查询"],
            ["系统接口", "/api/v1/admin/system/smtp/*、/api/v1/admin/demo-mode/*", "系统配置与演示模式控制"],
        ],
        [1500, 3600, 3400],
    )

    sect_pr = (
        "<w:sectPr>"
        "<w:pgSz w:w=\"11906\" w:h=\"16838\"/>"
        "<w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\" "
        "w:header=\"708\" w:footer=\"708\" w:gutter=\"0\"/>"
        "<w:cols w:space=\"708\"/>"
        "<w:docGrid w:linePitch=\"360\"/>"
        "</w:sectPr>"
    )

    body = "".join(doc.parts) + sect_pr
    return (
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        "<w:document xmlns:wpc=\"http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas\" "
        "xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\" "
        "xmlns:o=\"urn:schemas-microsoft-com:office:office\" "
        "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" "
        "xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\" "
        "xmlns:v=\"urn:schemas-microsoft-com:vml\" "
        "xmlns:wp14=\"http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing\" "
        "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" "
        "xmlns:w10=\"urn:schemas-microsoft-com:office:word\" "
        "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" "
        "xmlns:w14=\"http://schemas.microsoft.com/office/word/2010/wordml\" "
        "xmlns:w15=\"http://schemas.microsoft.com/office/word/2012/wordml\" "
        "xmlns:wpg=\"http://schemas.microsoft.com/office/word/2010/wordprocessingGroup\" "
        "xmlns:wpi=\"http://schemas.microsoft.com/office/word/2010/wordprocessingInk\" "
        "xmlns:wne=\"http://schemas.microsoft.com/office/word/2006/wordml\" "
        "xmlns:wps=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\" "
        "mc:Ignorable=\"w14 w15 wp14\">"
        f"<w:body>{body}</w:body></w:document>"
    )


def build_styles() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="宋体" w:cs="Times New Roman"/>
        <w:sz w:val="24"/>
        <w:szCs w:val="24"/>
        <w:lang w:val="en-US" w:eastAsia="zh-CN" w:bidi="ar-SA"/>
      </w:rPr>
    </w:rPrDefault>
    <w:pPrDefault>
      <w:pPr>
        <w:spacing w:after="120" w:line="360" w:lineRule="auto"/>
      </w:pPr>
    </w:pPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:qFormat/>
    <w:rPr>
      <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="宋体"/>
      <w:sz w:val="24"/>
      <w:szCs w:val="24"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Title">
    <w:name w:val="Title"/>
    <w:basedOn w:val="Normal"/>
    <w:qFormat/>
    <w:pPr>
      <w:jc w:val="center"/>
      <w:spacing w:before="240" w:after="240"/>
    </w:pPr>
    <w:rPr>
      <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="黑体"/>
      <w:b/>
      <w:bCs/>
      <w:sz w:val="36"/>
      <w:szCs w:val="36"/>
      <w:color w:val="1F1F1F"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:basedOn w:val="Normal"/>
    <w:next w:val="Normal"/>
    <w:uiPriority w:val="9"/>
    <w:qFormat/>
    <w:pPr>
      <w:spacing w:before="240" w:after="160"/>
      <w:outlineLvl w:val="0"/>
    </w:pPr>
    <w:rPr>
      <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="黑体"/>
      <w:b/>
      <w:bCs/>
      <w:sz w:val="30"/>
      <w:szCs w:val="30"/>
      <w:color w:val="1F3A5F"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading2">
    <w:name w:val="heading 2"/>
    <w:basedOn w:val="Normal"/>
    <w:next w:val="Normal"/>
    <w:uiPriority w:val="9"/>
    <w:qFormat/>
    <w:pPr>
      <w:spacing w:before="180" w:after="120"/>
      <w:outlineLvl w:val="1"/>
    </w:pPr>
    <w:rPr>
      <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="黑体"/>
      <w:b/>
      <w:bCs/>
      <w:sz w:val="26"/>
      <w:szCs w:val="26"/>
      <w:color w:val="2F4F6F"/>
    </w:rPr>
  </w:style>
</w:styles>
"""


def build_settings() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:settings xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:zoom w:percent="100"/>
  <w:defaultTabStop w:val="420"/>
  <w:characterSpacingControl w:val="doNotCompress"/>
  <w:compat/>
</w:settings>
"""


def build_content_types() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>
"""


def build_root_rels() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>
"""


def build_document_rels() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>
"""


def build_core_props() -> str:
    now = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
  xmlns:dc="http://purl.org/dc/elements/1.1/"
  xmlns:dcterms="http://purl.org/dc/terms/"
  xmlns:dcmitype="http://purl.org/dc/dcmitype/"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>校园二手交易系统概要设计说明书</dc:title>
  <dc:subject>概要设计</dc:subject>
  <dc:creator>Codex</dc:creator>
  <cp:keywords>校园二手交易系统,概要设计,Spring Boot,Vue</cp:keywords>
  <dc:description>根据当前项目实现整理的概要设计说明书。</dc:description>
  <cp:lastModifiedBy>Codex</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">{now}</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">{now}</dcterms:modified>
</cp:coreProperties>
"""


def build_app_props() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
  xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Codex</Application>
  <DocSecurity>0</DocSecurity>
  <ScaleCrop>false</ScaleCrop>
  <Company>OpenAI</Company>
  <LinksUpToDate>false</LinksUpToDate>
  <SharedDoc>false</SharedDoc>
  <HyperlinksChanged>false</HyperlinksChanged>
  <AppVersion>1.0</AppVersion>
</Properties>
"""


def write_docx() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(OUTPUT, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("[Content_Types].xml", build_content_types())
        zf.writestr("_rels/.rels", build_root_rels())
        zf.writestr("docProps/core.xml", build_core_props())
        zf.writestr("docProps/app.xml", build_app_props())
        zf.writestr("word/document.xml", build_content())
        zf.writestr("word/styles.xml", build_styles())
        zf.writestr("word/settings.xml", build_settings())
        zf.writestr("word/_rels/document.xml.rels", build_document_rels())


if __name__ == "__main__":
    write_docx()
    print(OUTPUT)
