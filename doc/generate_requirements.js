'use strict';
const fs = require('fs');
const path = require('path');
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  HeadingLevel, AlignmentType, TableOfContents, BorderStyle, WidthType,
  ShadingType, VerticalAlign, PageNumber, Header, Footer, PageBreak,
  LevelFormat, TabStopType, TabStopPosition,
} = require('docx');

// ── helpers ──────────────────────────────────────────────────────────────────

const CONTENT_WIDTH = 9026; // A4 1-inch margins in DXA
const GRAY_BG = 'D9D9D9';
const BLUE_BG = 'BDD7EE';
const HEADER_BG = '2E75B6';

const border = (color = 'AAAAAA') => ({ style: BorderStyle.SINGLE, size: 1, color });
const borders = (c) => {
  const b = border(c);
  return { top: b, bottom: b, left: b, right: b };
};

function h1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    children: [new TextRun({ text, font: 'SimHei', size: 32, bold: true })],
    spacing: { before: 360, after: 180 },
  });
}
function h2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    children: [new TextRun({ text, font: 'SimHei', size: 28, bold: true })],
    spacing: { before: 240, after: 120 },
  });
}
function h3(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    children: [new TextRun({ text, font: 'SimHei', size: 26, bold: true })],
    spacing: { before: 180, after: 80 },
  });
}
function para(text, opts = {}) {
  return new Paragraph({
    children: [new TextRun({ text, font: 'SimSun', size: 24, ...opts })],
    spacing: { before: 60, after: 60 },
    indent: { firstLine: 480 },
  });
}
function bullet(text) {
  return new Paragraph({
    numbering: { reference: 'bullets', level: 0 },
    children: [new TextRun({ text, font: 'SimSun', size: 24 })],
    spacing: { before: 40, after: 40 },
  });
}
function numbered(text) {
  return new Paragraph({
    numbering: { reference: 'numbers', level: 0 },
    children: [new TextRun({ text, font: 'SimSun', size: 24 })],
    spacing: { before: 40, after: 40 },
  });
}
function blank() {
  return new Paragraph({ children: [new TextRun('')], spacing: { before: 60, after: 60 } });
}
function pageBreak() {
  return new Paragraph({ children: [new PageBreak()] });
}

// Use-case table builder
function ucTable(rows) {
  const allBorders = borders('888888');
  const makeRow = (label, content, isHeader = false) =>
    new TableRow({
      children: [
        new TableCell({
          width: { size: 1800, type: WidthType.DXA },
          borders: allBorders,
          shading: { fill: isHeader ? BLUE_BG : GRAY_BG, type: ShadingType.CLEAR },
          margins: { top: 80, bottom: 80, left: 120, right: 120 },
          children: [new Paragraph({ children: [new TextRun({ text: label, font: 'SimHei', size: 22, bold: true })] })],
        }),
        new TableCell({
          width: { size: 7226, type: WidthType.DXA },
          borders: allBorders,
          margins: { top: 80, bottom: 80, left: 120, right: 120 },
          children: Array.isArray(content) ? content : [new Paragraph({ children: [new TextRun({ text: content, font: 'SimSun', size: 22 })] })],
        }),
      ],
    });

  return new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: [1800, 7226],
    rows: rows.map(([label, content]) => makeRow(label, content)),
  });
}

// Simple two-column info table for class descriptions
function classTable(title, fields) {
  const allBorders = borders('888888');
  const headerRow = new TableRow({
    children: [
      new TableCell({
        columnSpan: 2,
        borders: allBorders,
        shading: { fill: HEADER_BG, type: ShadingType.CLEAR },
        margins: { top: 80, bottom: 80, left: 120, right: 120 },
        children: [new Paragraph({ children: [new TextRun({ text: title, font: 'SimHei', size: 24, bold: true, color: 'FFFFFF' })] })],
      }),
    ],
  });
  const colHeaderRow = new TableRow({
    children: ['属性名称', '说明'].map((h) =>
      new TableCell({
        width: { size: CONTENT_WIDTH / 2, type: WidthType.DXA },
        borders: allBorders,
        shading: { fill: GRAY_BG, type: ShadingType.CLEAR },
        margins: { top: 60, bottom: 60, left: 120, right: 120 },
        children: [new Paragraph({ children: [new TextRun({ text: h, font: 'SimHei', size: 22, bold: true })] })],
      })
    ),
  });
  const dataRows = fields.map(([name, desc]) =>
    new TableRow({
      children: [name, desc].map((t) =>
        new TableCell({
          width: { size: CONTENT_WIDTH / 2, type: WidthType.DXA },
          borders: allBorders,
          margins: { top: 60, bottom: 60, left: 120, right: 120 },
          children: [new Paragraph({ children: [new TextRun({ text: t, font: 'SimSun', size: 22 })] })],
        })
      ),
    })
  );
  return new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: [CONTENT_WIDTH / 2, CONTENT_WIDTH / 2],
    rows: [headerRow, colHeaderRow, ...dataRows],
  });
}

// Multiline content helper for uc table cells
function lines(arr) {
  return arr.map((t) => new Paragraph({ children: [new TextRun({ text: t, font: 'SimSun', size: 22 })], spacing: { before: 30, after: 30 } }));
}

// ── Chapter 1: 引言 ───────────────────────────────────────────────────────────
const chapter1 = [
  h1('一、引言'),
  h2('1.1 立项背景'),
  para('随着高校在校学生规模的持续扩大，校园内二手物品的流通需求日益旺盛。大学生群体在学业周期结束后往往面临大量闲置物品的处置问题，包括教材、电子设备、生活用品、运动器材等。传统线下摆摊或依托社交媒体发帖的方式存在信息散乱、安全性低、成交效率差等痛点。同时，在校学生购置所需物品时也缺乏可信赖的渠道。针对上述校园场景的特殊性，构建一个专属于高校师生的二手交易平台具有重要的现实意义。'),
  para('本系统——校园二手交易系统，正是面向以上需求而规划设计的，旨在为校园用户提供安全、便捷、高效的二手商品发布、浏览、求购和交易服务。'),
  h2('1.2 立项原因概述'),
  para('当前市场上存在闲鱼、转转等通用型二手交易平台，但其面向社会大众开放，存在信任门槛高、学生群体特有场景支持不足等问题。校园二手交易平台通过实名注册审核机制，确保交易双方均为在校学生或教职人员，有效提升了交易安全性与信任感。同时，系统针对校园场景设计了宿舍送货、自提、面交等本地化配送方式，并提供了求购帖、个性化推荐、商品评论等功能，更好地满足了校园社区的交易特性。'),
  h2('1.3 UML 概述'),
  para('本文档采用 UML（统一建模语言，Unified Modeling Language）对系统进行面向对象分析与设计。UML 是第三代面向对象建模语言，由 Grady Booch、Ivar Jacobson 和 Jim Rumbaugh 联合开发，已成为 OMG 标准建模语言。'),
  para('UML 定义了以下主要模型图，本文档将重点使用：'),
  bullet('用例图（Use Case Diagram）：描述系统的参与者（Actor）及其与系统用例之间的交互关系，用于捕获功能需求。'),
  bullet('活动图（Activity Diagram）：描述业务流程或用例场景中的活动序列及控制流。'),
  bullet('顺序图（Sequence Diagram）：展示对象之间按时间顺序发生的消息交互，用于描述典型场景的动态行为。'),
  bullet('类图（Class Diagram）：展示系统中的类、属性、操作及其相互关系，是静态结构建模的核心工具。'),
  bullet('组件图（Component Diagram）：描述软件组件及其依赖关系。'),
  bullet('部署图（Deployment Diagram）：描述系统运行时的物理部署结构。'),
  h2('1.4 参考文献'),
  bullet('《面向对象需求分析文档-参考案例：超市管理系统需求规格说明书v2》'),
  bullet('Spring Boot 3.3 官方文档：https://docs.spring.io/spring-boot/docs/3.3.x/reference/html/'),
  bullet('MyBatis-Plus 官方文档：https://baomidou.com/'),
  bullet('Vue 3 官方文档：https://vuejs.org/'),
  bullet('《软件工程：实践者的研究方法》（第 8 版），Roger S. Pressman 著'),
  blank(),
];

// ── Chapter 3: 系统描述 ───────────────────────────────────────────────────────
const chapter3 = [
  h1('三、系统描述'),
  h2('3.1 系统概述'),
  para('校园二手交易系统是一个 B/S 架构的 Web 应用，分为前台用户端和后台管理端两大子系统。前台用户端面向学生用户，提供商品浏览、发布、交易、求购、个性化推荐、消息通知等核心功能；后台管理端面向管理员，提供注册申请审核、用户管理、商品监管、订单管理、公告管理以及数据统计与报表导出等运营功能。'),
  para('系统采用前后端分离架构，后端以 Spring Boot 3 构建 RESTful API 服务，前端以 Vue 3 + Element Plus 构建单页应用（SPA）。用户身份认证采用 JWT（JSON Web Token）机制，接口按照游客、注册用户、管理员三种角色进行权限控制。'),
  h2('3.2 系统总体结构'),
  para('系统总体上划分为以下几个功能模块：'),
  bullet('用户认证模块：注册申请（含学生证上传）、登录（含图形验证码）、JWT Token 签发与校验。'),
  bullet('商品管理模块：商品发布（含图片上传）、商品编辑、商品下架、商品列表/详情浏览、多维度搜索与筛选。'),
  bullet('商品评论模块：商品评论发布、回复、查看，评论消息通知。'),
  bullet('交易订单模块：订单创建、卖家确认、标记发货、买家确认完成、订单取消，订单状态流转记录。'),
  bullet('求购帖模块：求购帖发布、浏览、搜索、管理（上/下线）。'),
  bullet('个性化推荐模块：基于用户行为日志（浏览、搜索、购买等）的推荐列表生成、刷新与点击记录。'),
  bullet('通知模块：系统在关键业务事件（订单状态变更、注册结果等）发生时自动推送通知，支持站内通知与邮件通知。'),
  bullet('公告模块：管理员发布/下线系统公告，支持置顶与过期时间设置；用户端展示公告列表。'),
  bullet('文件存储模块：本地文件存储，支持学生证、头像、商品图片的上传与访问。'),
  bullet('管理后台模块：注册申请审核、用户/商品/订单/公告管理、数据看板、报表 CSV 导出。'),
  h2('3.3 各部分功能描述'),
  h3('3.3.1 前台用户子系统'),
  para('用户子系统提供学生用户的全部业务功能，主要包括：'),
  bullet('注册与登录：学生提交注册申请（填写基本信息并上传学生证），待管理员审核通过后激活账号；登录时需输入学号、密码及图形验证码。'),
  bullet('个人资料管理：用户可更新个人信息（姓名、联系方式、院系班级、宿舍地址等）并上传头像。'),
  bullet('商品发布与管理：用户可发布二手商品，设置分类、成色（全新/几乎全新/轻微使用/已使用/深度使用）、价格、交易方式（线下/线上/均可）、联系方式等；可对自己发布的商品进行编辑、下架或删除。'),
  bullet('商品浏览与搜索：未登录游客和已登录用户均可按关键词、分类、品牌、价格区间、成色、交易方式等条件搜索和筛选商品，支持按最新、价格升/降序排序。'),
  bullet('商品评论：已登录用户可对商品发表评论或回复他人评论；用户可查看自己收到的所有评论回复。'),
  bullet('交易订单：买家可对在售商品创建订单，指定收货人、联系方式、配送方式（宿舍送货/自提/面交）；卖家可确认订单、标记发货；买家确认收货后订单完成；任一方均可取消待确认订单。'),
  bullet('求购帖：用户可发布求购帖说明所需商品信息及期望价格范围，设置有效期；可管理自己的求购帖状态（开放/关闭/过期）。'),
  bullet('个性化推荐：系统根据用户行为数据生成个性化商品推荐列表，用户可刷新推荐；推荐理由包括相似分类、最近浏览、热销、关键词匹配、协同过滤等。'),
  bullet('消息通知：用户可查看系统发送的通知消息（订单状态变更通知、注册审核结果通知等），并标记已读。'),
  h3('3.3.2 后台管理子系统'),
  para('管理子系统面向管理员，分三个角色（超级管理员、审核员、运营员），主要包括：'),
  bullet('注册申请管理：查看学生注册申请列表，审阅学生证图片，执行批准或拒绝操作，批准后系统自动为该学生创建账户。'),
  bullet('用户管理：查看用户列表，启用/禁用用户账号，查看用户详情。'),
  bullet('商品管理：查看全量商品列表，对违规商品执行下架操作，可按状态、卖家学号等条件筛选。'),
  bullet('订单管理：查看全量订单列表，可强制取消异常订单，对已完成订单执行关闭存档操作。'),
  bullet('公告管理：创建/编辑系统公告，执行发布/下线操作，支持置顶和过期时间设置。'),
  bullet('数据统计看板：提供总览指标（注册用户数、在售商品数、订单数等）、订单趋势折线图、商品状态分布、分类销售排行、热搜关键词、用户增长趋势等统计图表。'),
  bullet('报表导出：支持将各统计数据导出为 CSV 格式文件，便于离线分析。'),
  blank(),
];

// ── Chapter 2: 项目概述 ───────────────────────────────────────────────────────
const chapter2 = [
  h1('二、项目概述'),
  h2('2.1 面向的用户'),
  para('本系统面向三类用户群体：'),
  bullet('注册学生用户（买家 / 卖家）：通过身份实名审核的在校学生，是系统的主要使用群体，可发布商品、浏览购买、发起求购、管理订单、查看通知等。'),
  bullet('游客（未登录访问者）：无需注册即可浏览公开商品列表、商品详情、求购帖列表及系统公告，但无法进行交易操作。'),
  bullet('管理员（后台运营人员）：分为超级管理员（super_admin）、审核员（auditor）和运营员（operator）三个角色，负责注册申请审核、用户管理、商品监管、订单管理、公告发布及数据统计分析。'),
  h2('2.2 实现目标'),
  numbered('构建一个面向高校学生的封闭式、实名制二手交易平台，确保交易双方身份可信。'),
  numbered('提供便捷的商品发布功能，支持多图上传、分类管理、成色标注、价格协商等。'),
  numbered('实现完整的交易订单闭环，包含创建订单、卖家确认、发货、买家完成、取消等全流程状态管理。'),
  numbered('提供求购帖功能，让有采购需求的用户主动发布需求，促进供需匹配。'),
  numbered('基于用户浏览、搜索、购买等行为数据，提供个性化商品推荐，提升用户发现优质商品的效率。'),
  numbered('提供完善的管理后台，支持数据看板、报表导出、用户/商品/订单管理等运营功能。'),
  numbered('系统具备良好的安全性，采用 JWT 鉴权、图形验证码、分角色权限控制等机制。'),
  h2('2.3 项目开发要求'),
  numbered('前后端分离架构：后端提供 RESTful API，前端独立部署，通过 Vite 代理与后端通信。'),
  numbered('接口规范统一：所有 API 遵循统一的响应格式封装（ApiResponse），HTTP 状态码语义明确。'),
  numbered('代码规范：遵循 Java 代码规范及 Vue 3 Composition API 最佳实践，命名清晰，注释完整。'),
  numbered('安全性要求：密码加密存储，Token 鉴权，接口按角色授权，防止越权访问。'),
  numbered('可扩展性：实体设计采用软删除，枚举值可扩展，支持后续功能迭代。'),
  numbered('数据完整性：关键业务操作记录日志（登录日志、管理员操作日志、订单状态变更日志）。'),
  h2('2.4 开发工具与技术栈'),
  bullet('后端：Java 17 + Spring Boot 3.3 + MyBatis-Plus + MySQL 8.x + JWT + Spring Security'),
  bullet('前端：Vue 3 + Vite + TypeScript + Pinia + Vue Router + Axios + Element Plus + ECharts'),
  bullet('构建工具：Maven（后端）、npm（前端）'),
  bullet('API 文档：Swagger UI（SpringDoc OpenAPI 3）'),
  bullet('开发 IDE：IntelliJ IDEA / VS Code'),
  bullet('版本管理：Git'),
  blank(),
];

// ── Chapter 4: 系统分析 ───────────────────────────────────────────────────────
const chapter4 = [
  h1('四、系统分析'),
  h2('4.1 用例图'),
  para('本系统包含三类参与者：游客（Guest）、注册用户（User）和管理员（Admin）。以下对各主要用例进行详细分析。'),

  // UC-01
  h3('1）用户注册申请'),
  ucTable([
    ['用例名称', '用户注册申请'],
    ['描述', '学生用户填写个人信息并上传学生证照片，提交注册申请，等待管理员审核。'],
    ['标识符', 'UC-01'],
    ['优先级', 'A（高）'],
    ['角色', '游客（未注册学生）'],
    ['前置条件', '用户已访问系统注册页面，尚未拥有账号。'],
    ['主事件流', lines([
      '1. 游客访问注册页面，用例开始。',
      '2. 游客上传学生证图片，系统返回文件 ID。',
      '3. 游客填写学号、真实姓名、性别、邮箱、手机号、密码、学院、专业、班级等信息。',
      '4. 游客提交注册申请，系统校验字段合法性。',
      '   A1：字段校验失败。',
      '5. 系统生成申请编号，将申请记录入库，状态设为"待审核"（pending）。',
      '6. 系统提示申请提交成功，等待审核。',
      '7. 用例结束。',
    ])],
    ['其他事件流', lines([
      'A1：字段校验失败',
      '  (1) 系统显示具体字段错误提示。',
      '  (2) 返回主事件流第 3 步。',
    ])],
    ['后置条件', '注册申请已保存至数据库，状态为"待审核"；管理员可在后台看到该申请。'],
    ['特殊需求', '学生证图片格式须为 JPG/PNG，大小不超过 10MB。'],
  ]),
  blank(),

  // UC-02
  h3('2）用户登录'),
  ucTable([
    ['用例名称', '用户登录'],
    ['描述', '已通过审核的注册用户使用学号、密码及图形验证码登录系统，获取 JWT Token。'],
    ['标识符', 'UC-02'],
    ['优先级', 'A（高）'],
    ['角色', '注册用户'],
    ['前置条件', '用户已拥有审核通过的账号。'],
    ['主事件流', lines([
      '1. 用户访问登录页面，请求图形验证码。',
      '2. 系统生成验证码图片，返回验证码 key 和图片。',
      '3. 用户输入学号、密码和验证码，提交登录请求。',
      '4. 系统校验验证码是否正确。',
      '   A1：验证码错误或过期。',
      '5. 系统校验学号和密码是否匹配。',
      '   A2：用户不存在或密码错误。',
      '   A3：账号已被禁用。',
      '6. 系统记录登录日志，签发 JWT Token。',
      '7. 前端存储 Token，跳转至首页。',
      '8. 用例结束。',
    ])],
    ['其他事件流', lines([
      'A1：验证码错误或过期',
      '  (1) 系统提示验证码无效，刷新验证码。',
      '  (2) 返回主事件流第 3 步。',
      'A2：学号或密码错误',
      '  (1) 系统提示"学号或密码不正确"。',
      '  (2) 返回主事件流第 3 步。',
      'A3：账号已被禁用',
      '  (1) 系统提示账号已被禁用，请联系管理员。',
      '  (2) 用例结束。',
    ])],
    ['后置条件', '用户成功登录，持有有效 JWT Token，登录日志已记录。'],
    ['特殊需求', 'Token 默认有效期为 7 天；验证码有效期为 5 分钟。'],
  ]),
  blank(),

  // UC-03
  h3('3）发布二手商品'),
  ucTable([
    ['用例名称', '发布二手商品'],
    ['描述', '已登录用户上传商品图片并填写商品信息，将商品发布至平台供其他用户浏览购买。'],
    ['标识符', 'UC-03'],
    ['优先级', 'A（高）'],
    ['角色', '注册用户（卖家）'],
    ['前置条件', '用户已登录，账号状态正常。'],
    ['主事件流', lines([
      '1. 用户进入发布商品页面，用例开始。',
      '2. 用户上传商品图片（可多张），系统返回各图片文件 ID。',
      '3. 用户填写商品信息：标题、分类、品牌、型号、描述、成色、价格、原价、库存、交易方式、是否可议价、联系方式、取货地址等。',
      '4. 用户选择封面图，设置发布状态（在售或草稿）。',
      '5. 用户提交商品信息，系统校验必填字段。',
      '   A1：必填字段缺失或格式错误。',
      '6. 系统保存商品信息及图片关联，商品进入在售（on_sale）状态。',
      '7. 系统记录用户行为日志（publish）。',
      '8. 系统返回商品详情，用例结束。',
    ])],
    ['其他事件流', lines([
      'A1：字段校验失败',
      '  (1) 系统提示具体字段错误信息。',
      '  (2) 返回主事件流第 3 步。',
    ])],
    ['后置条件', '商品已保存至数据库，在售状态的商品可在公开列表中被搜索和浏览。'],
    ['特殊需求', '商品图片最多支持 9 张；价格精度保留小数点后 2 位。'],
  ]),
  blank(),

  // UC-04
  h3('4）浏览与搜索商品'),
  ucTable([
    ['用例名称', '浏览与搜索商品'],
    ['描述', '游客或已登录用户通过关键词、分类、价格区间等条件搜索浏览在售二手商品，查看商品详情。'],
    ['标识符', 'UC-04'],
    ['优先级', 'A（高）'],
    ['角色', '游客 / 注册用户'],
    ['前置条件', '无（游客可直接访问）。'],
    ['主事件流', lines([
      '1. 用户访问商品列表页，用例开始。',
      '2. 用户（可选）输入关键词、选择分类、设置价格区间、选择成色、选择交易方式、选择排序方式。',
      '3. 系统根据筛选条件查询在售商品列表，分页返回结果。',
      '   A1：无匹配商品。',
      '4. 用户点击某商品，系统返回商品详情（含图片列表、卖家信息、评论数等）。',
      '5. 系统记录已登录用户的浏览行为日志（view）和搜索历史（search）。',
      '6. 系统增加商品浏览次数。',
      '7. 用例结束。',
    ])],
    ['其他事件流', lines([
      'A1：无匹配商品',
      '  (1) 系统返回空列表，提示"暂无相关商品"。',
      '  (2) 用例结束。',
    ])],
    ['后置条件', '用户已查看商品列表或详情；已登录用户的搜索和浏览记录已写入行为日志。'],
    ['特殊需求', '列表分页，默认每页 10 条；排序支持最新、价格升序、价格降序。'],
  ]),
  blank(),

  // UC-05
  h3('5）创建交易订单'),
  ucTable([
    ['用例名称', '创建交易订单'],
    ['描述', '买家对在售商品发起购买，创建订单，等待卖家确认。'],
    ['标识符', 'UC-05'],
    ['优先级', 'A（高）'],
    ['角色', '注册用户（买家）'],
    ['前置条件', '买家已登录；目标商品处于在售（on_sale）状态；买家非该商品卖家。'],
    ['主事件流', lines([
      '1. 买家在商品详情页点击"购买"，用例开始。',
      '2. 买家填写收货人姓名、联系电话、配送方式（宿舍送货/自提/面交）、收货地址、买家备注。',
      '3. 买家提交订单，系统校验商品库存是否充足。',
      '   A1：商品库存不足或已下架。',
      '4. 系统生成订单编号，保存订单记录，状态设为"待确认"（pending_confirm）。',
      '5. 系统将商品状态更新为"已预留"（reserved）。',
      '6. 系统向卖家发送"有新订单待确认"通知。',
      '7. 系统返回订单详情，用例结束。',
    ])],
    ['其他事件流', lines([
      'A1：商品不可购买',
      '  (1) 系统提示"该商品已下架或库存不足"。',
      '  (2) 返回商品详情页，用例结束。',
    ])],
    ['后置条件', '订单已创建，状态为"待确认"；商品状态已更新为"已预留"；卖家收到通知。'],
    ['特殊需求', '订单编号格式为时间戳+随机串，全局唯一。'],
  ]),
  blank(),

  // UC-06
  h3('6）订单状态管理'),
  ucTable([
    ['用例名称', '订单状态管理（确认/发货/完成/取消）'],
    ['描述', '卖家确认订单、标记发货；买家确认收货完成交易；买卖双方均可取消待确认订单；每次状态变更均记录日志并通知对方。'],
    ['标识符', 'UC-06'],
    ['优先级', 'A（高）'],
    ['角色', '注册用户（买家 / 卖家）'],
    ['前置条件', '相关订单已存在；操作方为该订单的买家或卖家。'],
    ['主事件流', lines([
      '（卖家确认）',
      '1. 卖家在订单列表找到待确认订单，点击"确认接单"，用例开始。',
      '2. 系统将订单状态更新为"待发货"（awaiting_delivery），记录确认时间。',
      '3. 系统向买家发送"卖家已确认，等待发货"通知。',
      '（卖家标记发货）',
      '4. 卖家点击"标记发货"，状态更新为"配送中"（delivering），记录发货时间。',
      '5. 系统向买家发送"商品已发货"通知。',
      '（买家确认收货）',
      '6. 买家确认收货，状态更新为"已完成"（completed），记录完成时间。',
      '7. 系统将商品状态更新为"已售出"（sold）。',
      '8. 系统记录购买行为日志（purchase）。',
      '9. 用例结束。',
    ])],
    ['其他事件流', lines([
      'A1：任一方取消订单（仅限 pending_confirm 状态）',
      '  (1) 操作方提交取消原因。',
      '  (2) 系统将订单状态更新为"已取消"（cancelled），记录取消方和原因。',
      '  (3) 系统将商品状态恢复为"在售"（on_sale）。',
      '  (4) 系统向对方发送"订单已取消"通知。',
    ])],
    ['后置条件', '订单状态已更新；相关状态变更日志已记录；双方均收到通知。'],
    ['特殊需求', '每次订单状态变更均写入 order_status_logs 表以便追溯。'],
  ]),
  blank(),

  // UC-07
  h3('7）发布求购帖'),
  ucTable([
    ['用例名称', '发布求购帖'],
    ['描述', '用户发布求购帖，说明所需商品信息及期望价格范围，吸引拥有该物品的卖家主动联系。'],
    ['标识符', 'UC-07'],
    ['优先级', 'B（中）'],
    ['角色', '注册用户'],
    ['前置条件', '用户已登录，账号状态正常。'],
    ['主事件流', lines([
      '1. 用户进入求购帖发布页面，用例开始。',
      '2. 用户填写求购标题、分类、品牌型号、描述、期望价格范围、联系方式、有效期。',
      '3. 用户提交求购帖，系统校验必填字段。',
      '   A1：字段校验失败。',
      '4. 系统保存求购帖，状态设为"开放"（open）。',
      '5. 系统记录用户行为日志（want_post）。',
      '6. 系统返回求购帖详情，用例结束。',
    ])],
    ['其他事件流', lines([
      'A1：字段校验失败',
      '  (1) 系统提示具体错误信息。',
      '  (2) 返回主事件流第 2 步。',
    ])],
    ['后置条件', '求购帖已发布，处于开放状态，可被其他用户浏览。'],
    ['特殊需求', '有效期过后系统自动将求购帖状态更新为"已过期"（expired）。'],
  ]),
  blank(),

  // UC-08
  h3('8）管理员审核注册申请'),
  ucTable([
    ['用例名称', '管理员审核注册申请'],
    ['描述', '管理员查看注册申请，审阅学生证照片及填写信息，执行批准或拒绝；批准后系统自动创建学生账户。'],
    ['标识符', 'UC-08'],
    ['优先级', 'A（高）'],
    ['角色', '管理员（auditor / super_admin）'],
    ['前置条件', '管理员已登录后台，存在待审核的注册申请。'],
    ['主事件流', lines([
      '1. 管理员打开注册申请管理页面，查看待审核列表，用例开始。',
      '2. 管理员点击某申请，查看详情（学生证图片、填写信息）。',
      '3. 管理员核实信息无误，点击"批准"，填写审核备注（可选）。',
      '4. 系统将申请状态更新为"已批准"（approved），记录审核人和审核时间。',
      '5. 系统自动为该学生创建用户账号，账号状态为"正常"（active）。',
      '6. 系统向申请人邮箱发送"注册成功"通知。',
      '7. 系统记录管理员操作日志。',
      '8. 用例结束。',
    ])],
    ['其他事件流', lines([
      'A1：管理员拒绝申请',
      '  (1) 管理员点击"拒绝"，填写拒绝理由（必填）。',
      '  (2) 系统将申请状态更新为"已拒绝"（rejected）。',
      '  (3) 系统向申请人邮箱发送拒绝原因通知。',
      '  (4) 系统记录操作日志，用例结束。',
    ])],
    ['后置条件', '申请状态已更新；若批准则用户账号已创建；申请人已收到审核结果通知。'],
    ['特殊需求', '审核员（auditor）和超级管理员（super_admin）均有权审核注册申请。'],
  ]),
  blank(),

  // UC-09
  h3('9）个性化推荐'),
  ucTable([
    ['用例名称', '个性化推荐'],
    ['描述', '系统根据用户行为数据生成个性化商品推荐列表，用户可查看和刷新推荐。'],
    ['标识符', 'UC-09'],
    ['优先级', 'B（中）'],
    ['角色', '注册用户'],
    ['前置条件', '用户已登录。'],
    ['主事件流', lines([
      '1. 用户访问"推荐"页面或触发刷新，用例开始。',
      '2. 系统根据用户行为日志（浏览、搜索、购买记录）计算推荐评分。',
      '3. 系统综合相似分类、最近浏览、热销、关键词匹配、协同过滤等策略生成推荐列表。',
      '4. 系统将推荐列表写入 user_recommendations 表，返回带推荐理由的商品列表。',
      '5. 用户点击推荐商品，系统记录点击事件。',
      '6. 用例结束。',
    ])],
    ['其他事件流', lines([
      'A1：用户行为数据不足',
      '  (1) 系统降级为热销商品推荐（hot_sale 策略）。',
    ])],
    ['后置条件', '推荐列表已保存；点击记录已更新。'],
    ['特殊需求', '推荐列表有过期时间；单次刷新最多 50 条。'],
  ]),
  blank(),

  // UC-10
  h3('10）数据统计与报表导出'),
  ucTable([
    ['用例名称', '数据统计与报表导出'],
    ['描述', '管理员查看运营数据看板（用户增长、订单趋势、热搜关键词、分类销售排行等），并支持导出 CSV 报表。'],
    ['标识符', 'UC-10'],
    ['优先级', 'B（中）'],
    ['角色', '管理员（super_admin / auditor / operator）'],
    ['前置条件', '管理员已登录后台。'],
    ['主事件流', lines([
      '1. 管理员进入数据看板页面，用例开始。',
      '2. 系统展示总览指标：总用户数、今日新增、在售商品数、总订单数、已完成订单数等。',
      '3. 管理员选择时间范围，系统返回订单趋势折线图数据。',
      '4. 系统返回商品状态分布统计、分类销售排行、热搜关键词排行、用户增长趋势。',
      '5. 管理员点击"导出报表"，系统生成 CSV 并返回下载链接。',
      '6. 用例结束。',
    ])],
    ['其他事件流', lines([
      'A1：时间范围参数非法',
      '  (1) 系统返回参数校验错误提示，用例结束。',
    ])],
    ['后置条件', '统计数据已展示；CSV 报表已生成可下载。'],
    ['特殊需求', '订单趋势最长 30 天；分类销售排行最长 365 天；导出文件名含时间戳。'],
  ]),
  blank(),

  h2('4.2 活动图描述'),
  h3('4.2.1 商品发布流程'),
  para('商品发布活动流程：用户进入发布页面 → 上传商品图片（系统返回文件ID）→ 填写商品基本信息（标题/分类/成色/价格等）→ 填写交易信息（交易方式/联系方式/取货地址）→ 选择封面图并设置发布状态 → 提交表单（系统校验）→ 保存商品（在售状态）→ 记录用户行为日志 → 完成发布。若校验失败则返回表单修正。'),
  h3('4.2.2 订单全流程活动'),
  para('完整订单活动流程：买家浏览商品 → 点击购买 → 填写收货信息 → 提交订单（系统检查库存）→ 创建订单（待确认）→ 商品预留 → 卖家收到通知 → 卖家确认接单（待发货）→ 买家收到通知 → 卖家标记发货（配送中）→ 买家确认收货（已完成）→ 商品标记已售 → 记录购买行为。任一方在待确认阶段均可取消订单，商品状态恢复在售。'),
  h3('4.2.3 注册审核流程'),
  para('注册审核活动流程：学生访问注册页 → 上传学生证 → 填写申请信息 → 提交申请 → 系统生成申请记录（待审核）→ 管理员查看申请列表 → 管理员查阅学生证及信息 → 管理员执行批准/拒绝 → 若批准：系统创建用户账号，向学生发送注册成功通知；若拒绝：向学生发送拒绝原因通知 → 流程结束。'),

  h2('4.3 时序图描述'),
  h3('4.3.1 用户登录时序'),
  para('参与对象：User（前端）、UserAuthController、UserService、UserMapper、JwtUtil、LoginLogService。'),
  para('时序流程：User 发送 GET /captcha → 系统生成验证码图片及 key 返回；User 提交 POST /login（学号、密码、验证码、key）→ UserAuthController 调用 UserService.login() → 校验验证码 → UserMapper 查询用户 → 校验密码哈希 → JwtUtil 生成 Token → LoginLogService 记录登录日志 → 返回 Token 至前端。'),
  h3('4.3.2 创建订单时序'),
  para('参与对象：Buyer（前端）、UserOrderController、UserOrderService、ItemMapper、OrderMapper、NotificationService。'),
  para('时序流程：Buyer 发送 POST /orders（itemId、收货信息）→ UserOrderController 调用 UserOrderService.createOrder() → ItemMapper 查询商品状态及库存 → 校验可购买性 → OrderMapper 插入订单（pending_confirm）→ ItemMapper 更新商品状态为 reserved → 记录订单状态变更日志 → NotificationService 向卖家发送通知 → 返回订单详情至买家前端。'),

  h2('4.4 类分析'),
  para('基于系统功能需求，识别出以下核心领域类：'),
  blank(),
  classTable('User — 用户', [
    ['userId', '用户唯一标识（自增主键）'],
    ['applicationId', '关联的注册申请 ID'],
    ['studentNo', '学号（唯一索引）'],
    ['email / passwordHash', '邮箱 / 密码哈希（BCrypt）'],
    ['realName / gender / phone', '真实姓名 / 性别 / 手机号'],
    ['qqNo / wechatNo', 'QQ 号 / 微信号（可选联系方式）'],
    ['avatarFileId', '头像文件 ID（关联 MediaFile）'],
    ['collegeName / majorName / className', '学院 / 专业 / 班级'],
    ['dormitoryAddress', '宿舍地址'],
    ['accountStatus', '账号状态（active / disabled）'],
    ['lastLoginAt / createdAt / updatedAt / deletedAt', '各时间戳'],
  ]),
  blank(),
  classTable('RegistrationApplication — 注册申请', [
    ['applicationId / applicationNo', '申请唯一标识 / 业务流水号'],
    ['studentNo / realName / gender / email / phone', '申请人基本信息'],
    ['passwordHash', '申请时设置的密码哈希'],
    ['collegeName / majorName / className', '院系班级'],
    ['studentCardFileId', '学生证图片文件 ID'],
    ['status', '审核状态（pending / approved / rejected）'],
    ['reviewerAdminId / reviewRemark / reviewedAt', '审核人 / 备注 / 时间'],
    ['submittedAt / updatedAt', '提交时间 / 更新时间'],
  ]),
  blank(),
  classTable('Item — 商品', [
    ['itemId / sellerUserId / categoryId', '商品ID / 卖家ID / 分类ID'],
    ['title / brand / model / description', '标题 / 品牌 / 型号 / 描述'],
    ['conditionLevel', '成色（new / almost_new / lightly_used / used / well_used）'],
    ['price / originalPrice / stock', '售价 / 原价 / 库存'],
    ['tradeMode', '交易方式（offline / online / both）'],
    ['negotiable', '是否可议价'],
    ['contactPhone / contactQq / contactWechat / pickupAddress', '联系方式 / 取货地址'],
    ['status', '状态（draft / on_sale / reserved / sold / off_shelf / deleted）'],
    ['publishedAt / soldAt / viewCount / commentCount', '发布时间 / 售出时间 / 浏览次数 / 评论数'],
  ]),
  blank(),
  classTable('TradeOrder — 交易订单', [
    ['orderId / orderNo', '订单ID / 业务流水号'],
    ['buyerUserId / sellerUserId', '买家ID / 卖家ID'],
    ['orderStatus', '状态（pending_confirm / awaiting_delivery / delivering / completed / cancelled / closed）'],
    ['deliveryType', '配送方式（dorm_delivery / self_pickup / face_to_face）'],
    ['receiverName / receiverPhone / deliveryAddress', '收货人姓名 / 手机 / 地址'],
    ['totalAmount', '订单总金额'],
    ['buyerRemark / sellerRemark', '买家 / 卖家备注'],
    ['cancelledBy / cancelReason', '取消方 / 取消原因'],
    ['confirmedAt / deliveredAt / completedAt / cancelledAt', '各关键时间节点'],
  ]),
  blank(),
  classTable('WantedPost — 求购帖', [
    ['wantedPostId / requesterUserId / categoryId', '帖ID / 发帖人ID / 分类ID'],
    ['title / brand / model / description', '标题 / 品牌 / 型号 / 描述'],
    ['expectedPriceMin / expectedPriceMax', '期望价格范围（最低 / 最高）'],
    ['contactPhone / contactQq / contactWechat', '联系方式'],
    ['status', '状态（open / closed / expired）'],
    ['expiresAt / viewCount', '过期时间 / 浏览次数'],
  ]),
  blank(),
  classTable('ItemComment — 商品评论', [
    ['commentId / itemId / commenterUserId', '评论ID / 商品ID / 评论者ID'],
    ['parentCommentId / replyToUserId', '父评论ID / 被回复用户ID'],
    ['content / status', '内容 / 状态（normal / deleted）'],
    ['createdAt / updatedAt / deletedAt', '各时间戳'],
  ]),
  blank(),
  classTable('Notification — 消息通知', [
    ['notificationId / receiverUserId / receiverEmail', '通知ID / 接收用户ID / 接收邮箱'],
    ['senderAdminId', '发送管理员ID（系统触发时为空）'],
    ['channel', '通知渠道（in_app / email）'],
    ['businessType', '业务类型（order_confirmed / order_cancelled / registration_approved 等）'],
    ['title / content', '通知标题 / 内容'],
    ['sendStatus', '发送状态（pending / sent / failed）'],
    ['sentAt / readAt / createdAt', '发送时间 / 已读时间 / 创建时间'],
  ]),
  blank(),
  classTable('UserRecommendation — 个性化推荐', [
    ['recommendationId / userId / itemId', '推荐ID / 用户ID / 商品ID'],
    ['recommendScore', '推荐评分'],
    ['reasonCode', '推荐理由（similar_category / recent_view / hot_sale / keyword_match / collaborative_filtering / manual）'],
    ['isClicked / generatedAt / expiresAt', '是否已点击 / 生成时间 / 过期时间'],
  ]),
  blank(),
  classTable('Admin — 管理员', [
    ['adminId / adminNo / adminName / email', '管理员ID / 编号 / 姓名 / 邮箱'],
    ['passwordHash', '密码哈希'],
    ['roleCode', '角色（super_admin / auditor / operator）'],
    ['accountStatus / lastLoginAt', '账号状态 / 最后登录时间'],
    ['createdAt / updatedAt', '各时间戳'],
  ]),
  blank(),

  h2('4.5 类设计'),
  h3('4.5.1 主要类关系说明'),
  bullet('User 与 Item：一对多。一个用户（卖家）可发布多件商品，通过 Item.sellerUserId 关联。'),
  bullet('User 与 TradeOrder：用户可作为买家或卖家参与多笔订单，通过 TradeOrder.buyerUserId / sellerUserId 关联。'),
  bullet('Item 与 ItemComment：一件商品可有多条评论（一对多），评论支持树形嵌套（parentCommentId 自关联）。'),
  bullet('Item 与 ItemImage：一件商品可关联多张图片（一对多），通过 ItemImage.itemId 关联。'),
  bullet('User 与 UserRecommendation：一个用户有多条推荐记录（一对多），推荐记录关联具体商品。'),
  bullet('User 与 UserBehaviorLog：一个用户有多条行为日志（一对多），日志可关联商品或求购帖。'),
  bullet('RegistrationApplication 与 User：一对一，申请批准后对应创建一个用户，通过 User.applicationId 关联。'),

  h2('4.6 系统组件框图'),
  para('后端组件结构（Controller → Service → Mapper → Database）：'),
  bullet('用户端 Controller：UserAuthController、UserItemController、UserOrderController、UserWantedPostController、UserRecommendationController、UserNotificationController、UserItemCommentController、UserProfileController。'),
  bullet('管理端 Controller：AdminAuthController、AdminRegistrationApplicationController、AdminUserManagementController、AdminItemManagementController、AdminOrderManagementController、AdminAnnouncementController、AdminDashboardController、AdminReportExportController。'),
  bullet('公开 Controller：PublicItemController、PublicWantedPostController、PublicAnnouncementController、PublicItemCategoryController、PublicFileController、PublicRegistrationController。'),
  bullet('Service 层：对应各 Controller 的业务逻辑实现类，包含事务管理与跨实体操作。'),
  bullet('安全组件：JwtTokenProvider（Token 签发/验证）、JwtAuthenticationFilter（请求拦截）、Spring Security 角色权限控制。'),
  bullet('存储组件：LocalFileStorageService（本地文件上传与静态访问）。'),
  bullet('通知组件：NotificationService（站内通知）、邮件通知服务（可选 SMTP 配置）。'),
  para('前端组件结构：'),
  bullet('路由层（Vue Router）：公开路由 / 用户路由（需登录）/ 管理员路由（需管理员 Token）。'),
  bullet('状态层（Pinia）：用户信息、认证 Token 全局状态管理。'),
  bullet('网络层（Axios + 拦截器）：统一 API 请求封装，自动附加 Authorization 头。'),
  bullet('视图层：公开页（商品列表、详情、求购帖、公告）、用户中心（个人资料、我的商品、我的订单、求购帖、推荐、通知）、管理后台（注册审核、用户/商品/订单管理、看板、报表）。'),

  h2('4.7 系统部署图'),
  bullet('客户端：用户通过 PC 或移动端浏览器访问，支持主流现代浏览器。'),
  bullet('Nginx（反向代理 + 静态服务器）：托管前端 Vue 构建产物（dist/），并反向代理 /api/* 请求至 Spring Boot 应用，同时提供 /uploads/* 的静态文件访问。'),
  bullet('Spring Boot 应用服务器：运行于 JVM，默认监听 8080 端口，处理全部业务 API 请求。'),
  bullet('MySQL 8 数据库服务器：存储所有业务数据，推荐通过内网连接，数据库名 campus_secondhand。'),
  bullet('本地文件存储（storage/）：存储上传的学生证、头像、商品图片文件，通过 Nginx 对外暴露静态访问。'),
  blank(),
];

// ── Chapter 5: 系统接口 ───────────────────────────────────────────────────────
const chapter5 = [
  h1('五、系统接口'),
  h2('5.1 用户接口'),
  para('系统前端采用 Vue 3 单页应用（SPA）构建，提供以下交互界面：'),
  bullet('公开页面：商品列表页（支持搜索、筛选、排序、分页）、商品详情页（含图片轮播、评论区、卖家信息）、求购帖列表页、求购帖详情页、系统公告列表页。'),
  bullet('用户认证页面：登录页（学号/密码/图形验证码）、注册申请页（分步填写基本信息与上传学生证）。'),
  bullet('用户中心页面：个人资料编辑页、我发布的商品列表（含发布/编辑/下架操作）、我的订单列表（买入/卖出两类，含状态操作按钮）、我的求购帖列表（含管理操作）、我的推荐页（个性化推荐列表，支持刷新）、消息通知列表（含未读标记）。'),
  bullet('管理后台页面：管理员登录页、注册申请列表与审核详情页、用户管理列表页、商品管理列表页、订单管理列表页、公告管理列表与编辑页、数据看板页（ECharts 图表）、报表导出页。'),
  para('所有页面均采用 Element Plus 组件库，具备表单验证、加载状态、错误提示、空状态等标准交互反馈；响应式布局支持桌面端浏览器访问。'),
  h2('5.2 硬件接口'),
  para('本系统为纯 Web 应用，对硬件无特殊依赖，用户仅需具备以下条件即可正常访问：'),
  bullet('终端设备：能运行主流浏览器（Chrome 90+、Firefox 88+、Edge 90+、Safari 14+）的 PC 或移动设备。'),
  bullet('网络环境：能够访问部署服务器所在网络（校园局域网或公网）的任意网络连接。'),
  para('服务器端硬件要求（推荐配置）：'),
  bullet('应用服务器：2 核及以上 CPU，4GB 及以上内存，SSD 硬盘（用于存储上传文件，建议 100GB 以上）。'),
  bullet('数据库服务器：可与应用服务器合并部署（小规模），或独立部署 MySQL 8 实例。'),
  blank(),
];

// ── Chapter 6: 性能需求 ───────────────────────────────────────────────────────
const chapter6 = [
  h1('六、性能需求'),
  h2('6.1 精度要求'),
  bullet('商品价格及订单金额：精度保留小数点后 2 位有效数字（如 ¥99.50）。'),
  bullet('推荐评分：系统内部计算精度保留小数点后 4 位，接口输出保留 2 位。'),
  bullet('统计数据：百分比类数据保留 2 位小数，数量类数据为整数。'),
  h2('6.2 时间特征'),
  bullet('普通查询接口（商品列表、订单列表、通知列表等）响应时间：正常负载下不超过 500ms。'),
  bullet('文件上传（图片）接口：10MB 以内文件上传并返回文件 ID，响应时间不超过 3 秒。'),
  bullet('数据统计接口（看板数据、报表导出）：响应时间不超过 5 秒。'),
  bullet('JWT Token 校验及权限验证：每次请求增加时延不超过 10ms。'),
  bullet('数据库写入操作（订单创建、状态变更等）：不超过 200ms。'),
  h2('6.3 灵活性'),
  bullet('操作系统兼容性：后端应用可运行于 Windows、Linux、macOS（JVM 跨平台）；推荐部署于 Linux 服务器。'),
  bullet('浏览器兼容性：前端支持所有主流现代浏览器，无需安装插件。'),
  bullet('并发能力：系统设计支持 50 个并发用户的正常使用，如需更高并发可通过增加应用实例水平扩展。'),
  bullet('配置灵活性：JWT 有效期、文件存储路径、邮件服务器配置、分页默认值等均通过 application.yml 外部配置，无需修改代码即可调整。'),
  blank(),
];

// ── Chapter 7: 软件属性 ───────────────────────────────────────────────────────
const chapter7 = [
  h1('七、软件属性'),
  h2('7.1 系统安全性'),
  bullet('身份认证：用户和管理员登录均采用 JWT（JSON Web Token）机制，Token 存储于前端 localStorage，每次请求通过 Authorization: Bearer 头传递，后端 JwtAuthenticationFilter 自动校验有效性与过期时间。'),
  bullet('密码安全：所有密码均使用 BCrypt 哈希算法加密存储，不存储明文密码；登录时通过哈希比对验证。'),
  bullet('图形验证码：用户登录接口增加图形验证码校验，防止暴力破解和自动化登录攻击。'),
  bullet('角色权限控制：后端使用 Spring Security @PreAuthorize 注解实现基于角色的访问控制（RBAC）；管理员接口要求 Admin Token，用户接口要求 User Token，公开接口无需认证。'),
  bullet('实名制交易：用户需通过学生证实名审核方可注册，降低交易风险，提升平台可信度。'),
  bullet('SQL 注入防护：使用 MyBatis-Plus 框架参数化查询，从根本上防止 SQL 注入攻击。'),
  bullet('文件安全：上传文件类型白名单校验（仅允许图片格式），文件名随机化处理，避免路径遍历攻击。'),
  bullet('数据备份建议：定期对 MySQL 数据库进行全量备份（推荐每日备份），对上传文件目录进行增量备份，以防数据丢失。'),
  h2('7.2 可维护性'),
  bullet('代码结构清晰：采用标准 MVC 分层架构（Controller / Service / Mapper），职责分离，便于定位和修改。'),
  bullet('统一响应格式：所有 API 返回标准 ApiResponse 格式（code / message / data），前端统一处理逻辑，降低维护成本。'),
  bullet('枚举管理：业务状态值均以 Java 枚举类型定义（OrderStatus、ItemStatus、AdminRoleCode 等），避免魔法数字，便于扩展。'),
  bullet('软删除设计：核心实体（User、Item、TradeOrder 等）使用 deletedAt 字段实现软删除，数据可追溯，误删可恢复。'),
  bullet('日志记录：系统记录登录日志（login_logs）、管理员操作日志（admin_operation_logs）、订单状态变更日志（order_status_logs），便于事后审计和故障排查。'),
  bullet('API 文档：集成 Swagger UI（SpringDoc OpenAPI 3），接口文档自动生成，前后端协作效率高，维护成本低。'),
  bullet('配置外部化：关键配置（数据库连接、JWT 密钥、文件存储路径、邮件服务器等）均通过 application.yml 管理，生产环境可通过环境变量覆盖，便于运维。'),
  blank(),
];

// ── Chapter 8: 其他需求 ───────────────────────────────────────────────────────
const chapter8 = [
  h1('八、其他需求'),
  h2('8.1 数据库需求'),
  para('系统使用 MySQL 8.x 作为持久化数据库，数据库名为 campus_secondhand，主要数据表及说明如下：'),
  bullet('users：用户表，记录注册用户的账号、个人信息、账号状态等。'),
  bullet('registration_applications：注册申请表，记录学生注册申请及审核结果。'),
  bullet('admins：管理员表，记录后台管理员账号、角色、状态等。'),
  bullet('items：商品表，记录在售及历史商品信息，使用 deleted_at 软删除。'),
  bullet('item_categories：商品分类表，支持父子两级分类结构。'),
  bullet('item_images：商品图片表，与 items 一对多关联。'),
  bullet('item_comments：商品评论表，支持树形嵌套回复结构。'),
  bullet('trade_orders：交易订单表，记录完整订单信息及状态。'),
  bullet('order_items：订单商品关联表，记录订单包含的商品及快照价格。'),
  bullet('order_status_logs：订单状态变更日志表，记录每次状态变更的操作方、时间和备注。'),
  bullet('wanted_posts：求购帖表，记录用户发布的求购信息。'),
  bullet('announcements：系统公告表，支持置顶和过期时间。'),
  bullet('notifications：消息通知表，记录发送给用户的各类通知。'),
  bullet('user_recommendations：个性化推荐表，记录系统为每个用户生成的推荐商品列表。'),
  bullet('user_behavior_logs：用户行为日志表，记录浏览、搜索、评论、购买等行为，用于推荐算法输入。'),
  bullet('media_files：媒体文件表，记录上传文件的元数据（文件名、路径、大小、类型等）。'),
  bullet('login_logs：登录日志表，记录用户登录时间、IP 地址、登录结果等。'),
  bullet('admin_operation_logs：管理员操作日志表，记录管理员的关键操作行为。'),
  bullet('system_settings：系统配置表，存储可在线调整的全局配置项（键值对形式）。'),
  para('数据库访问通过 MyBatis-Plus 框架进行，使用连接池（HikariCP）管理连接，关键查询字段均已建立索引（如 student_no、item status、order status、user_id 等），保证查询性能。'),
  h2('8.2 系统操作要求'),
  bullet('数据库管理：使用 MySQL 8.x，可通过 MySQL Workbench、Navicat 等工具进行日常维护；系统提供初始化 SQL 脚本（schema.sql）用于建库建表。'),
  bullet('应用部署：后端打包为可执行 JAR 文件，通过 java -jar 启动；前端通过 npm run build 生成静态文件，由 Nginx 托管；提供 README.md 详细说明部署步骤。'),
  bullet('初始数据：系统提供初始超级管理员账号（adminNo: admin001，默认密码可在首次启动时配置），用于初始化后台管理功能。'),
  bullet('文件存储维护：定期清理孤立文件（未被任何实体引用的上传文件），建议通过定时任务执行。'),
  bullet('配置修改：修改 application.yml 后需重启应用服务器使配置生效；部分系统配置（system_settings 表）可通过管理后台在线修改无需重启。'),
  h2('8.3 故障及其处理'),
  bullet('数据库故障：建议配置 MySQL 主从复制，主库故障时切换至从库，避免服务中断；定期备份确保数据可恢复。'),
  bullet('应用崩溃：建议使用进程守护工具（如 systemd、Supervisor）在应用意外退出时自动重启；关键操作已通过 Spring 事务注解保证原子性，避免数据不一致。'),
  bullet('文件系统故障：上传文件存储目录建议定期备份至异地存储；磁盘空间不足时系统会返回相应错误，需及时清理或扩容。'),
  bullet('Token 失效处理：前端检测到 401 未授权响应时，自动清除本地 Token 并跳转至登录页，引导用户重新登录。'),
  bullet('数据备份建议：重要数据（用户信息、交易订单等）用户应通过管理后台报表导出功能定期备份，数据库层面建议每日全量备份并保留最近 30 天的备份文件。'),
  blank(),
];

// ── Cover page ────────────────────────────────────────────────────────────────
const coverPage = [
  new Paragraph({
    children: [new TextRun('')],
    spacing: { before: 2000 },
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: '校园二手交易系统', font: 'SimHei', size: 56, bold: true })],
    spacing: { before: 240, after: 120 },
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: '需求规格说明书', font: 'SimHei', size: 48, bold: true })],
    spacing: { before: 120, after: 600 },
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: 'Software Requirements Specification', font: 'Times New Roman', size: 28, italics: true, color: '666666' })],
    spacing: { before: 0, after: 1200 },
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: '版本：V1.0', font: 'SimSun', size: 24 })],
    spacing: { before: 120, after: 120 },
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: '文档状态：正式发布', font: 'SimSun', size: 24 })],
    spacing: { before: 120, after: 120 },
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: '编制日期：' + new Date().toLocaleDateString('zh-CN'), font: 'SimSun', size: 24 })],
    spacing: { before: 120, after: 120 },
  }),
  pageBreak(),
];

// ── Assemble Document ─────────────────────────────────────────────────────────
const doc = new Document({
  numbering: {
    config: [
      {
        reference: 'bullets',
        levels: [{ level: 0, format: LevelFormat.BULLET, text: '\u2022', alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 360, hanging: 360 } } } }],
      },
      {
        reference: 'numbers',
        levels: [{ level: 0, format: LevelFormat.DECIMAL, text: '%1.', alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 480, hanging: 360 } } } }],
      },
    ],
  },
  styles: {
    default: {
      document: { run: { font: 'SimSun', size: 24 } },
    },
    paragraphStyles: [
      {
        id: 'Heading1', name: 'Heading 1',
        basedOn: 'Normal', next: 'Normal',
        run: { font: 'SimHei', size: 32, bold: true, color: '1F3864' },
        paragraph: { spacing: { before: 360, after: 180 } },
      },
      {
        id: 'Heading2', name: 'Heading 2',
        basedOn: 'Normal', next: 'Normal',
        run: { font: 'SimHei', size: 28, bold: true, color: '2E75B6' },
        paragraph: { spacing: { before: 240, after: 120 } },
      },
      {
        id: 'Heading3', name: 'Heading 3',
        basedOn: 'Normal', next: 'Normal',
        run: { font: 'SimHei', size: 26, bold: true, color: '2F5597' },
        paragraph: { spacing: { before: 180, after: 80 } },
      },
    ],
  },
  sections: [
    {
      properties: {
        page: {
          margin: { top: 1440, right: 1080, bottom: 1440, left: 1080 },
        },
      },
      children: [
        ...coverPage,
        ...chapter1,
        pageBreak(),
        ...chapter2,
        pageBreak(),
        ...chapter3,
        pageBreak(),
        ...chapter4,
        pageBreak(),
        ...chapter5,
        pageBreak(),
        ...chapter6,
        pageBreak(),
        ...chapter7,
        pageBreak(),
        ...chapter8,
      ],
    },
  ],
});

// ── Write file ────────────────────────────────────────────────────────────────
const outPath = path.join(__dirname, '校园二手交易系统需求规格说明书.docx');
Packer.toBuffer(doc).then((buffer) => {
  fs.writeFileSync(outPath, buffer);
  console.log('Done! Written to:', outPath);
}).catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});
