const fs = require("fs");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, HeadingLevel, BorderStyle, WidthType,
  ShadingType, PageNumber, PageBreak, TableOfContents
} = require("docx");

// ── Helpers ──────────────────────────────────────────────────────────────
const PAGE_W = 11906; // A4
const PAGE_H = 16838;
const MARGIN = 1440;
const CONTENT_W = PAGE_W - 2 * MARGIN; // 9026

const border = { style: BorderStyle.SINGLE, size: 1, color: "000000" };
const borders = { top: border, bottom: border, left: border, right: border };
const noBorders = {
  top: { style: BorderStyle.NONE, size: 0 },
  bottom: { style: BorderStyle.NONE, size: 0 },
  left: { style: BorderStyle.NONE, size: 0 },
  right: { style: BorderStyle.NONE, size: 0 },
};
const cellMargins = { top: 60, bottom: 60, left: 100, right: 100 };

function textRun(text, opts = {}) {
  return new TextRun({ text, font: "SimSun", size: 24, ...opts });
}
function boldRun(text, opts = {}) {
  return textRun(text, { bold: true, ...opts });
}
function cell(children, width, opts = {}) {
  const paragraphs = Array.isArray(children) && children[0] instanceof Paragraph
    ? children
    : [new Paragraph({ alignment: AlignmentType.CENTER, children: Array.isArray(children) ? children : [children] })];
  return new TableCell({
    borders,
    width: { size: width, type: WidthType.DXA },
    margins: cellMargins,
    verticalAlign: "center",
    ...opts,
    children: paragraphs,
  });
}
function leftCell(children, width, opts = {}) {
  const paragraphs = Array.isArray(children) && children[0] instanceof Paragraph
    ? children
    : [new Paragraph({ spacing: { line: 360 }, children: Array.isArray(children) ? children : [children] })];
  return new TableCell({
    borders,
    width: { size: width, type: WidthType.DXA },
    margins: cellMargins,
    verticalAlign: "center",
    ...opts,
    children: paragraphs,
  });
}
function headerCell(children, width, opts = {}) {
  return cell(children, width, {
    shading: { fill: "D9E2F3", type: ShadingType.CLEAR },
    ...opts,
  });
}
function emptyPara() {
  return new Paragraph({ children: [] });
}
function heading1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 200, after: 200 },
    children: [new TextRun({ text, font: "SimHei", size: 32, bold: true })],
  });
}
function heading2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 160, after: 160 },
    children: [new TextRun({ text, font: "SimHei", size: 28, bold: true })],
  });
}
function heading3(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    spacing: { before: 120, after: 120 },
    children: [new TextRun({ text, font: "SimHei", size: 24, bold: true })],
  });
}
function bodyPara(text, opts = {}) {
  return new Paragraph({
    spacing: { line: 360 },
    indent: { firstLine: 480 },
    ...opts,
    children: [textRun(text)],
  });
}
function bodyParaNoIndent(text, opts = {}) {
  return new Paragraph({
    spacing: { line: 360 },
    ...opts,
    children: [textRun(text)],
  });
}

// ── Module info table (per PDF template) ─────────────────────────────────
function moduleInfoTable(name, id) {
  const col1 = 1800, col2 = 3000, col3 = 1800, col4 = 2426;
  return new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: [col1, col2, col3, col4],
    rows: [
      new TableRow({ children: [
        headerCell([boldRun("模块名称")], col1),
        cell([textRun(name)], col2),
        headerCell([boldRun("模块说明编号")], col3),
        cell([textRun(id)], col4),
      ]}),
    ],
  });
}
function triggerTable(trigger) {
  return new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: [1800, 7226],
    rows: [
      new TableRow({ children: [
        headerCell([boldRun("启动条件")], 1800),
        leftCell([textRun(trigger)], 7226),
      ]}),
    ],
  });
}
function ioTable(input, func, output) {
  const cw = [2400, 3826, 2800];
  return new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: cw,
    rows: [
      new TableRow({ children: [
        headerCell([boldRun("输入")], cw[0]),
        headerCell([boldRun("功能")], cw[1]),
        headerCell([boldRun("输出")], cw[2]),
      ]}),
      new TableRow({ children: [
        leftCell([textRun(input)], cw[0]),
        leftCell([textRun(func)], cw[1]),
        leftCell([textRun(output)], cw[2]),
      ]}),
    ],
  });
}

// Build a full module design section
function moduleSection(name, id, trigger, input, func, output, flowSteps, interfaceDesc) {
  const children = [];
  children.push(heading3(name));
  children.push(moduleInfoTable(name, id));
  children.push(emptyPara());
  children.push(triggerTable(trigger));
  children.push(emptyPara());
  children.push(ioTable(input, func, output));
  children.push(emptyPara());
  children.push(new Paragraph({ spacing: { before: 100 }, alignment: AlignmentType.CENTER,
    children: [boldRun("算法流程逻辑")] }));
  flowSteps.forEach(s => {
    children.push(bodyParaNoIndent(s));
  });
  children.push(emptyPara());
  children.push(new Paragraph({ spacing: { before: 100 }, alignment: AlignmentType.CENTER,
    children: [boldRun("程序界面说明")] }));
  interfaceDesc.forEach(s => {
    children.push(bodyPara(s));
  });
  children.push(emptyPara());
  return children;
}

// ── Cover section ───────────────────────────────────────────────────────
function buildCoverSection() {
  return [
    emptyPara(), emptyPara(), emptyPara(), emptyPara(), emptyPara(),
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 400 },
      children: [new TextRun({ text: "校园二手交易系统", font: "SimHei", size: 44, bold: true })] }),
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 600 },
      children: [new TextRun({ text: "详细设计说明书", font: "SimHei", size: 40, bold: true })] }),
    emptyPara(), emptyPara(),
    new Paragraph({ alignment: AlignmentType.CENTER,
      children: [textRun("本报告说明确定软件系统的总体结构、数据结构及其他全局性的考虑，", { size: 22 })] }),
    new Paragraph({ alignment: AlignmentType.CENTER,
      children: [textRun("所设计的系统应覆盖既定的软件需求，经过评审通过后，本说明是后续实现的根据。", { size: 22 })] }),
    emptyPara(), emptyPara(),
    // Document index table
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 200, after: 200 },
      children: [boldRun("详细设计说明书目录", { size: 26 })] }),
    new Table({
      width: { size: 6000, type: WidthType.DXA },
      columnWidths: [4200, 1800],
      rows: [
        new TableRow({ children: [
          headerCell([boldRun("章节")], 4200),
          headerCell([boldRun("文档序号")], 1800),
        ]}),
        new TableRow({ children: [
          cell([textRun("1. 引言")], 4200),
          cell([textRun("D1")], 1800),
        ]}),
        new TableRow({ children: [
          cell([textRun("2. 程序系统结构图")], 4200),
          cell([textRun("D2")], 1800),
        ]}),
        new TableRow({ children: [
          cell([textRun("3. 程序设计说明")], 4200),
          cell([textRun("D3")], 1800),
        ]}),
      ],
    }),
    emptyPara(), emptyPara(), emptyPara(),
    new Paragraph({ alignment: AlignmentType.CENTER,
      children: [textRun("本说明书由设计人员根据软件需求说明书制定，由主程序员和程序员实施，")] }),
    new Paragraph({ alignment: AlignmentType.CENTER,
      children: [textRun("供测试人员及维护人员参考。")] }),
    emptyPara(),
    new Paragraph({ alignment: AlignmentType.RIGHT,
      children: [textRun("程序员：_______________")] }),
    new Paragraph({ alignment: AlignmentType.RIGHT, spacing: { before: 100 },
      children: [textRun("(签名)")] }),
    new Paragraph({ alignment: AlignmentType.RIGHT, spacing: { before: 100 },
      children: [textRun("日    期：  2026 年 4 月")] }),
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

// ── Section 1: Introduction ─────────────────────────────────────────────
function buildIntroSection() {
  // Section header table
  const headerTbl = new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: [1800, 3200, 1800, 2226],
    rows: [
      new TableRow({ children: [
        headerCell([boldRun("主文档名称")], 1800),
        cell([textRun("详细设计说明书")], 3200),
        headerCell([boldRun("主文档编号")], 1800),
        cell([textRun("D")], 2226),
      ]}),
      new TableRow({ children: [
        headerCell([boldRun("子文档名称")], 1800),
        cell([textRun("引言")], 3200),
        headerCell([boldRun("子文档序号")], 1800),
        cell([textRun("D1")], 2226),
      ]}),
      new TableRow({ children: [
        headerCell([boldRun("编者")], 1800),
        cell([textRun("开发团队")], 3200),
        headerCell([boldRun("编写日期")], 1800),
        cell([textRun("2026年4月")], 2226),
      ]}),
    ],
  });

  return [
    heading1("1. 引言"),
    headerTbl,
    emptyPara(),
    heading2("（1）编写目的"),
    bodyPara("在校园二手交易系统概要设计说明书的基础上，本详细设计说明书对系统各模块的程序结构和具体实现进行详细说明。包括如何把该系统划分成若干模块、决定各个模块之间的接口、模块之间传递的信息，以及数据结构、模块结构的设计等。"),
    bodyPara("在本阶段中，确定应该如何具体地实现所要求的系统，以便在编码阶段可以把这个描述直接翻译成用具体的程序语言书写的程序。主要的工作有：根据在软件需求说明书中所描述的数据、功能、运行、性能需求，并依照概要设计说明书所确定的处理流程、总体结构和模块外部设计，设计软件系统的结构设计，逐个模块的程序描述（包括各模块的功能、性能、输入、输出、算法、程序逻辑、接口等）。"),
    emptyPara(),
    heading2("（2）项目背景"),
    bodyPara("开发软件名称：校园二手交易系统（Campus Second-hand Trading System）"),
    bodyPara("项目任务提出者：校方教务管理部门"),
    bodyPara("项目开发者：校园二手交易系统开发团队"),
    bodyPara("用户：在校师生、管理员"),
    bodyPara("实现软件单位：校园软件开发团队"),
    emptyPara(),
    heading2("（3）符号、缩略语和定义"),
    bodyPara("CSTHS：Campus Second-hand Trading Handling System，校园二手交易系统"),
    bodyPara("JWT：JSON Web Token，用于用户认证的令牌机制"),
    bodyPara("RBAC：Role-Based Access Control，基于角色的访问控制"),
    bodyPara("API：Application Programming Interface，应用程序编程接口"),
    bodyPara("CRUD：Create / Read / Update / Delete，增删改查基本操作"),
    bodyPara("DTO：Data Transfer Object，数据传输对象"),
    bodyPara("VO：Value Object，视图对象"),
    bodyPara("B/S：Browser/Server，浏览器/服务器架构"),
    emptyPara(),
    heading2("（4）参考资料"),
    bodyPara("校园二手交易系统项目需求说明书"),
    bodyPara("校园二手交易系统概要设计说明书"),
    bodyPara("校园二手交易系统数据库设计文档"),
    bodyPara("Spring Boot 3.3 官方文档"),
    bodyPara("MyBatis-Plus 官方文档"),
    bodyPara("Vue 3 + Element Plus 官方文档"),
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

// ── Section 2: System Structure ─────────────────────────────────────────
function buildStructureSection() {
  const headerTbl = new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: [1800, 3200, 1800, 2226],
    rows: [
      new TableRow({ children: [
        headerCell([boldRun("主文档名称")], 1800),
        cell([textRun("详细设计说明书")], 3200),
        headerCell([boldRun("主文档编号")], 1800),
        cell([textRun("D")], 2226),
      ]}),
      new TableRow({ children: [
        headerCell([boldRun("子文档名称")], 1800),
        cell([textRun("程序系统结构图")], 3200),
        headerCell([boldRun("子文档序号")], 1800),
        cell([textRun("D2")], 2226),
      ]}),
      new TableRow({ children: [
        headerCell([boldRun("编者")], 1800),
        cell([textRun("开发团队")], 3200),
        headerCell([boldRun("编写日期")], 1800),
        cell([textRun("2026年4月")], 2226),
      ]}),
    ],
  });

  // System description paragraphs
  const structDesc = [
    bodyPara("校园二手交易系统采用B/S架构，后端基于Spring Boot 3.3 + MyBatis-Plus + MySQL + JWT实现RESTful API服务，前端基于Vue 3 + TypeScript + Element Plus + ECharts构建单页应用（SPA）。系统按角色和业务划分为三大子系统：公共接口模块（Public）、用户模块（User）和管理员模块（Admin）。"),
    bodyPara("公共接口模块（Public）面向所有访客和已登录用户，提供注册申请、商品浏览与搜索、求购帖浏览、公告查看、商品分类查询和文件上传等功能，不要求用户登录即可访问。"),
    bodyPara("用户模块（User）面向已通过注册审核的校园用户，提供身份认证（登录/验证码）、个人资料管理、商品发布与管理、订单管理（买家/卖家双视角）、求购帖发布与管理、评论与回复、站内通知中心以及个性化推荐等功能。"),
    bodyPara("管理员模块（Admin）面向系统管理员，提供后台登录认证、注册申请审核、用户管理（禁用/启用）、商品审核与下架、订单管理（取消/关闭）、公告管理（发布/下线）、数据仪表盘（概览、趋势图、分类排行、热搜词、用户增长）以及报表CSV导出等功能。"),
  ];

  // Module composition table
  const modules = [
    ["00", "校园二手交易系统", "campus_secondhand", "campus_secondhand", "campus_secondhand"],
    ["01", "公共-注册申请", "public_registration", "public_registration", "public_registration"],
    ["02", "公共-商品浏览", "public_item_browse", "public_item_browse", "public_item_browse"],
    ["03", "公共-求购帖浏览", "public_wanted_post", "public_wanted_post", "public_wanted_post"],
    ["04", "公共-公告浏览", "public_announcement", "public_announcement", "public_announcement"],
    ["05", "公共-商品分类", "public_item_category", "public_item_category", "public_item_category"],
    ["06", "公共-文件上传", "public_file_upload", "public_file_upload", "public_file_upload"],
    ["07", "用户-身份认证", "user_auth", "user_auth", "user_auth"],
    ["08", "用户-个人资料", "user_profile", "user_profile", "user_profile"],
    ["09", "用户-商品管理", "user_item", "user_item", "user_item"],
    ["10", "用户-订单管理", "user_order", "user_order", "user_order"],
    ["11", "用户-求购帖管理", "user_wanted_post", "user_wanted_post", "user_wanted_post"],
    ["12", "用户-评论管理", "user_comment", "user_comment", "user_comment"],
    ["13", "用户-通知中心", "user_notification", "user_notification", "user_notification"],
    ["14", "用户-个性化推荐", "user_recommendation", "user_recommendation", "user_recommendation"],
    ["15", "管理-身份认证", "admin_auth", "admin_auth", "admin_auth"],
    ["16", "管理-注册审核", "admin_registration", "admin_registration", "admin_registration"],
    ["17", "管理-用户管理", "admin_user_mgmt", "admin_user_mgmt", "admin_user_mgmt"],
    ["18", "管理-商品管理", "admin_item_mgmt", "admin_item_mgmt", "admin_item_mgmt"],
    ["19", "管理-订单管理", "admin_order_mgmt", "admin_order_mgmt", "admin_order_mgmt"],
    ["20", "管理-公告管理", "admin_announcement", "admin_announcement", "admin_announcement"],
    ["21", "管理-数据仪表盘", "admin_dashboard", "admin_dashboard", "admin_dashboard"],
    ["22", "管理-报表导出", "admin_report_export", "admin_report_export", "admin_report_export"],
  ];

  const cw = [700, 2000, 2200, 2200, 1926];
  const modRows = modules.map(m => new TableRow({
    children: [
      cell([textRun(m[0])], cw[0]),
      cell([textRun(m[1])], cw[1]),
      cell([textRun(m[2], { size: 18 })], cw[2]),
      cell([textRun(m[3], { size: 18 })], cw[3]),
      cell([textRun(m[4], { size: 18 })], cw[4]),
    ],
  }));

  const modTable = new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: cw,
    rows: [
      new TableRow({ children: [
        headerCell([boldRun("序号")], cw[0]),
        headerCell([boldRun("程序模块名称")], cw[1]),
        headerCell([boldRun("标识符")], cw[2]),
        headerCell([boldRun("程序设计说明编号")], cw[3]),
        headerCell([boldRun("实现文档编号")], cw[4]),
      ]}),
      ...modRows,
    ],
  });

  return [
    heading1("2. 程序系统结构图"),
    headerTbl,
    emptyPara(),
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 200, after: 200 },
      children: [boldRun("校园二手交易系统程序系统结构图", { size: 26 })] }),
    ...structDesc,
    emptyPara(),
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 200, after: 200 },
      children: [boldRun("程 序 组 成", { size: 26 })] }),
    modTable,
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

// ── Section 3: Module Design Details ────────────────────────────────────
function buildModuleDesignSection() {
  const headerTbl = new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: [1800, 3200, 1800, 2226],
    rows: [
      new TableRow({ children: [
        headerCell([boldRun("主文档名称")], 1800),
        cell([textRun("详细设计说明书")], 3200),
        headerCell([boldRun("主文档编号")], 1800),
        cell([textRun("D")], 2226),
      ]}),
      new TableRow({ children: [
        headerCell([boldRun("子文档名称")], 1800),
        cell([textRun("程序设计说明")], 3200),
        headerCell([boldRun("子文档序号")], 1800),
        cell([textRun("D3")], 2226),
      ]}),
      new TableRow({ children: [
        headerCell([boldRun("编者")], 1800),
        cell([textRun("开发团队")], 3200),
        headerCell([boldRun("编写日期")], 1800),
        cell([textRun("2026年4月")], 2226),
      ]}),
    ],
  });

  const allModules = [];

  // ── 01 公共-注册申请 ──
  allModules.push(...moduleSection(
    "公共-注册申请", "public_registration",
    "访客在注册页面填写个人信息并上传学生证照片后，点击\"提交注册申请\"按钮",
    "学号、真实姓名、性别、邮箱、手机号、密码、学院、专业、班级、学生证图片文件ID",
    "校验学号和邮箱唯一性，对密码进行BCrypt加密，生成唯一申请编号，创建待审核的注册申请记录",
    "申请编号、申请状态（pending）、提交时间",
    [
      "1. 前端校验表单必填字段完整性",
      "2. 调用文件上传接口上传学生证图片，获得fileId",
      "3. POST /api/v1/public/registration-applications 提交申请",
      "4. 后端Service校验student_no和email在registration_applications及users表中无重复",
      "5. 对明文密码执行BCrypt哈希",
      "6. 生成唯一application_no（格式：REG+时间戳+随机数）",
      "7. 插入registration_applications表，status设为pending",
      "8. 返回RegistrationApplicationSubmitResponse（含申请编号和状态）",
    ],
    [
      "前端注册页面（RegisterView.vue）提供表单，包含学号、姓名、性别下拉框、邮箱、手机、密码、确认密码、学院、专业、班级输入框，以及学生证图片上传区域。表单底部设有\"提交申请\"按钮。提交成功后跳转至注册结果页（RegisterResultView.vue），显示申请编号和等待审核提示。",
    ]
  ));

  // ── 02 公共-商品浏览 ──
  allModules.push(...moduleSection(
    "公共-商品浏览", "public_item_browse",
    "用户访问首页或商品列表页面，可输入关键词搜索、按分类/价格/成色/交易方式筛选",
    "分类ID、关键词、品牌、价格区间（min/max）、成色、交易方式、排序方式、页码、每页数量",
    "根据筛选条件组合查询在售商品，支持全文搜索，记录搜索历史，分页返回商品列表；查看详情时增加浏览计数并记录用户行为",
    "分页商品列表（含标题、价格、封面图、卖家信息等）或单个商品详情（含所有图片、卖家联系方式、评论数）",
    [
      "1. 前端构造查询参数（keyword, categoryId, priceMin, priceMax, conditionLevel, tradeMode, sortBy, page, size）",
      "2. GET /api/v1/public/items 请求商品列表",
      "3. 后端PublicItemServiceImpl构建MyBatis-Plus LambdaQueryWrapper",
      "4. 若keyword非空，使用MySQL FULLTEXT全文索引搜索标题、品牌、型号、描述",
      "5. 按category_id, price区间, condition_level, trade_mode依次叠加过滤条件",
      "6. 仅查询status=on_sale的商品",
      "7. 根据sortBy决定排序：latest(发布时间降序)、price_asc、price_desc、默认(综合)",
      "8. 执行分页查询，关联查询封面图片和卖家基本信息",
      "9. 若用户已登录，异步记录搜索历史到search_histories表",
      "10. 查看详情时：GET /api/v1/public/items/{itemId}，浏览计数+1，记录view行为到user_behavior_logs表",
    ],
    [
      "前端首页（HomeView.vue）展示搜索栏和热门商品网格。商品列表页（ItemsView.vue）左侧为筛选面板（分类树、价格滑块、成色选择、交易方式），右侧为商品卡片网格，支持分页切换和排序方式选择。商品详情页（ItemDetailView.vue）展示商品大图轮播、标题、价格、成色标签、卖家信息、联系方式、评论列表和发表评论区。",
    ]
  ));

  // ── 03 公共-求购帖浏览 ──
  allModules.push(...moduleSection(
    "公共-求购帖浏览", "public_wanted_post",
    "用户访问求购帖列表页面，可按关键词搜索和分类筛选",
    "关键词、分类ID、价格区间、排序方式、页码、每页数量",
    "根据筛选条件查询状态为open的求购帖，支持全文搜索，分页返回求购帖列表；查看详情时增加浏览计数",
    "分页求购帖列表（含标题、期望价格、发布者信息）或单个求购帖详情",
    [
      "1. GET /api/v1/public/wanted-posts 传入筛选参数",
      "2. 后端PublicWantedPostServiceImpl构建查询条件",
      "3. 若keyword非空，使用FULLTEXT索引搜索title、brand、model、description",
      "4. 仅查询status=open且未过期的求购帖",
      "5. 分页查询并关联发布者基本信息",
      "6. 查看详情时：GET /api/v1/public/wanted-posts/{id}，浏览计数+1",
    ],
    [
      "求购帖列表页（WantedPostsView.vue）提供搜索框和分类筛选器，以卡片形式展示求购帖列表，每张卡片显示标题、期望价格区间和发布者信息。详情页（WantedPostDetailView.vue）展示求购帖完整信息和联系方式。",
    ]
  ));

  // ── 04 公共-公告浏览 ──
  allModules.push(...moduleSection(
    "公共-公告浏览", "public_announcement",
    "用户访问公告页面或首页公告区域",
    "页码、每页数量",
    "查询已发布且未过期的公告列表，置顶公告优先排列",
    "分页公告列表（含标题、内容摘要、发布时间、是否置顶）",
    [
      "1. GET /api/v1/public/announcements?page=1&size=5",
      "2. PublicAnnouncementServiceImpl查询publish_status=published且未超过expire_at的公告",
      "3. 按is_pinned降序、published_at降序排列",
      "4. 分页返回公告列表",
    ],
    [
      "公告列表页（AnnouncementsView.vue）以列表形式展示公告标题和发布时间，置顶公告带有置顶标签。首页（HomeView.vue）上方区域展示最新公告滚动条。",
    ]
  ));

  // ── 05 用户-身份认证 ──
  allModules.push(...moduleSection(
    "用户-身份认证", "user_auth",
    "用户在登录页面输入学号、密码和验证码后点击\"登录\"按钮",
    "学号（studentNo）、密码（password）、验证码（captcha）、验证码Key（captchaKey）",
    "验证图形验证码有效性，根据学号查找用户，BCrypt校验密码，检查账户状态，生成JWT令牌，记录登录日志",
    "JWT访问令牌（accessToken）、用户基本信息（学号、姓名、头像URL）",
    [
      "1. GET /api/v1/user/auth/captcha 获取Base64验证码图片和captchaKey",
      "2. 用户输入学号、密码、验证码",
      "3. POST /api/v1/user/auth/login 提交登录请求",
      "4. LoginCaptchaService验证captchaKey对应的验证码是否匹配（不区分大小写）",
      "5. 验证码通过后，根据studentNo从users表查询用户",
      "6. BCryptPasswordEncoder.matches()校验密码",
      "7. 检查account_status是否为active",
      "8. JwtTokenProvider生成JWT令牌（包含userId、studentNo、角色信息，有效期86400秒）",
      "9. 更新users.last_login_at",
      "10. 插入login_logs表记录登录成功",
      "11. 返回UserLoginResponse（含token和用户基本信息）",
    ],
    [
      "用户登录页面（LoginView.vue）包含学号输入框、密码输入框、验证码输入框和验证码图片区域（点击可刷新），底部设有\"登录\"和\"去注册\"按钮。登录成功后将JWT存入Pinia store和localStorage，路由跳转至首页。",
    ]
  ));

  // ── 06 用户-个人资料 ──
  allModules.push(...moduleSection(
    "用户-个人资料", "user_profile",
    "已登录用户点击\"个人中心\"进入个人资料页面",
    "真实姓名、邮箱、手机号、QQ号、微信号、学院、专业、班级、宿舍地址（编辑时）；旧密码、新密码（修改密码时）；头像图片文件（上传头像时）",
    "查询/更新用户个人资料；BCrypt校验旧密码后更新密码；上传头像文件并关联到用户记录",
    "完整的用户个人资料信息（含头像URL），或操作成功提示",
    [
      "1. GET /api/v1/user/profile 获取当前用户资料",
      "2. 后端从SecurityContext中获取UserPrincipal，查询users表",
      "3. 编辑资料：PUT /api/v1/user/profile 提交UpdateUserProfileRequest",
      "4. 后端校验字段合法性，更新users表对应字段",
      "5. 修改密码：PUT /api/v1/user/profile/password",
      "6. BCrypt验证旧密码，加密新密码后更新password_hash",
      "7. 上传头像：POST /api/v1/user/profile/avatar (multipart/form-data)",
      "8. FileStorageService将图片存储到 storage/avatars/yyyy/MM/{uuid}.{ext}",
      "9. 插入media_files表，更新users.avatar_file_id",
    ],
    [
      "个人资料页面（ProfileView.vue）顶部展示用户头像（可点击更换）和基本信息，下方为可编辑表单，包含真实姓名、邮箱、手机号、QQ号、微信号、学院、专业、班级、宿舍地址等字段。页面提供\"保存修改\"按钮和\"修改密码\"弹窗入口。",
    ]
  ));

  // ── 07 用户-商品管理 ──
  allModules.push(...moduleSection(
    "用户-商品管理", "user_item",
    "已登录用户在\"我的商品\"页面管理自己发布的商品，或在\"发布商品\"页面创建新商品",
    "商品信息（分类ID、标题、品牌、型号、描述、成色、价格、原价、库存、交易方式、是否可议价、联系方式、取货地址、商品图片文件ID列表、封面图ID）",
    "创建/更新/删除商品：校验用户身份，保存商品信息和图片关联，管理商品状态；列表查询：分页获取当前用户的商品列表",
    "商品详情（含所有图片URL、分类名称、状态）或分页商品列表",
    [
      "1. 上传商品图片：POST /api/v1/user/items/images (multipart/form-data)",
      "2. FileStorageService存储到 storage/item-images/yyyy/MM/{uuid}.{ext}",
      "3. 创建商品：POST /api/v1/user/items 提交SaveItemRequest",
      "4. 后端验证categoryId有效，seller_user_id设为当前用户ID",
      "5. 插入items表，状态为on_sale时自动设置published_at",
      "6. 根据imageFileIds批量插入item_images表，设置sort_order和is_cover",
      "7. 更新商品：PUT /api/v1/user/items/{itemId}，验证商品属于当前用户",
      "8. 删除商品：DELETE /api/v1/user/items/{itemId}，软删除（设置deleted_at、status=deleted）",
      "9. 查询列表：GET /api/v1/user/items?status=on_sale&page=1&size=10",
      "10. 查询详情：GET /api/v1/user/items/{itemId}",
    ],
    [
      "\"发布商品\"页面（PublishItemView.vue）提供完整的商品发布表单，包括分类下拉选择、标题、品牌、型号输入框、富文本描述、成色下拉框、价格/原价输入、库存、交易方式单选、是否可议价开关、联系方式（手机/QQ/微信）、取货地址、多图上传区域（支持拖拽排序和设置封面）。\"我的商品\"页面（MyItemsView.vue）以表格形式展示商品列表，支持按状态筛选，每行提供编辑和删除操作按钮。",
    ]
  ));

  // ── 08 用户-订单管理 ──
  allModules.push(...moduleSection(
    "用户-订单管理", "user_order",
    "买家在商品详情页点击\"购买\"或卖家在订单列表中操作订单状态流转",
    "创建订单：商品ID、数量、收货人姓名、收货人手机、配送方式、配送地址、买家备注；操作订单：订单ID、操作备注/取消原因",
    "创建订单（校验库存、计算金额、生成订单号）；查询订单列表（买家/卖家视角）；订单状态流转（确认/发货/完成/取消）",
    "订单详情（含订单号、状态、金额、商品快照、买卖双方信息、状态变更日志）或分页订单列表",
    [
      "1. 创建订单：POST /api/v1/user/orders 提交CreateOrderRequest",
      "2. 后端查询商品，验证status=on_sale且库存充足",
      "3. 生成唯一order_no（格式：ORD+时间戳+随机数）",
      "4. 创建订单记录（orders表），状态为pending_confirm",
      "5. 创建订单快照（order_items表），记录当时的商品标题和价格",
      "6. 扣减商品库存，库存为0时将商品状态改为sold",
      "7. 插入order_status_logs记录状态变更",
      "8. 发送站内通知给卖家",
      "9. 卖家确认：POST /api/v1/user/orders/{id}/confirm，状态→awaiting_delivery",
      "10. 卖家发货：POST /api/v1/user/orders/{id}/deliver，状态→delivering",
      "11. 买家确认收货：POST /api/v1/user/orders/{id}/complete，状态→completed",
      "12. 取消订单：POST /api/v1/user/orders/{id}/cancel，恢复库存，状态→cancelled",
      "13. 每次状态变更均插入order_status_logs并发送通知",
    ],
    [
      "订单管理页面（OrdersView.vue）提供买家/卖家视角切换标签，表格展示订单列表，包含订单号、商品标题、金额、状态、创建时间等列。每行根据当前状态显示可执行的操作按钮（确认/发货/完成/取消）。订单详情弹窗展示完整的订单信息、商品快照和状态变更时间线。",
    ]
  ));

  // ── 09 用户-求购帖管理 ──
  allModules.push(...moduleSection(
    "用户-求购帖管理", "user_wanted_post",
    "已登录用户在\"我的求购\"页面管理求购帖，或发布新的求购帖",
    "求购帖信息（分类ID、标题、品牌、型号、描述、期望价格区间、联系方式、状态、过期时间）",
    "创建/更新/删除求购帖，分页查询当前用户的求购帖列表",
    "求购帖详情或分页求购帖列表",
    [
      "1. 创建：POST /api/v1/user/wanted-posts 提交SaveWantedPostRequest",
      "2. 后端设置requester_user_id为当前用户，插入wanted_posts表",
      "3. 更新：PUT /api/v1/user/wanted-posts/{id}，验证帖子属于当前用户",
      "4. 删除：DELETE /api/v1/user/wanted-posts/{id}，软删除",
      "5. 列表：GET /api/v1/user/wanted-posts?status=open&page=1&size=10",
      "6. 详情：GET /api/v1/user/wanted-posts/{id}",
    ],
    [
      "\"我的求购\"页面（MyWantedPostsView.vue）以卡片或表格形式展示用户发布的求购帖，每个条目显示标题、期望价格、状态和过期时间，支持编辑和删除操作。发布求购帖表单包含分类选择、标题、描述、品牌、型号、期望价格区间、联系方式和过期时间设置。",
    ]
  ));

  // ── 10 用户-评论管理 ──
  allModules.push(...moduleSection(
    "用户-评论管理", "user_comment",
    "用户在商品详情页发表评论或回复已有评论，或在\"收到的评论\"页面查看",
    "商品ID、评论内容（发表评论时）；父评论ID、回复内容（回复评论时）",
    "创建评论/回复评论（关联到商品，更新评论计数）；查询收到的评论列表",
    "评论详情（含评论者信息、内容、时间）或分页收到的评论列表",
    [
      "1. 发表评论：POST /api/v1/user/items/{itemId}/comments 提交CreateItemCommentRequest",
      "2. 后端设置commenter_user_id，插入item_comments表",
      "3. 更新items.comment_count计数",
      "4. 若评论了他人商品，发送站内通知给卖家",
      "5. 回复评论：POST /api/v1/user/comments/{commentId}/reply",
      "6. 设置parent_comment_id和reply_to_user_id",
      "7. 发送站内通知给被回复者",
      "8. 查询收到的评论：GET /api/v1/user/comments/received?page=1&size=10",
      "9. 查询当前用户商品下收到的所有评论和回复",
    ],
    [
      "商品详情页（ItemDetailView.vue）底部评论区展示已有评论列表（支持嵌套回复），提供评论输入框和提交按钮。\"收到的评论\"页面（ReceivedCommentsView.vue）以列表形式展示其他用户对当前用户商品的评论，每条评论显示来源商品、评论者、内容和时间，支持快速回复。",
    ]
  ));

  // ── 11 用户-通知中心 ──
  allModules.push(...moduleSection(
    "用户-通知中心", "user_notification",
    "已登录用户访问通知中心页面查看站内消息",
    "阅读状态过滤（unread/all）、页码、每页数量（列表时）；通知ID（标记已读时）",
    "分页查询当前用户的站内通知列表；标记单条/全部通知为已读",
    "分页通知列表（含标题、内容、业务类型、已读状态、时间）或标记已读结果",
    [
      "1. 查询通知列表：GET /api/v1/user/notifications?readStatus=unread&page=1&size=10",
      "2. 后端根据receiver_user_id和channel=site查询notifications表",
      "3. 可按readStatus过滤（unread: read_at IS NULL）",
      "4. 按created_at降序分页返回",
      "5. 标记已读：POST /api/v1/user/notifications/{id}/read",
      "6. 设置notifications.read_at为当前时间",
      "7. 全部已读：POST /api/v1/user/notifications/read-all",
      "8. 批量更新当前用户所有未读通知的read_at字段",
    ],
    [
      "通知中心页面（NotificationsView.vue）以列表形式展示站内通知，未读通知加粗显示或带有未读标记。页面顶部提供\"全部已读\"按钮和已读/未读过滤切换。每条通知显示标题、摘要内容、业务类型标签和时间。",
    ]
  ));

  // ── 12 用户-个性化推荐 ──
  allModules.push(...moduleSection(
    "用户-个性化推荐", "user_recommendation",
    "已登录用户访问\"为你推荐\"页面，或系统后台定期刷新推荐数据",
    "页码、每页数量（查询时）；刷新数量limit（刷新时）；推荐ID（点击记录时）",
    "基于用户行为日志（浏览、搜索、评论、购买等）计算推荐得分，生成个性化商品推荐列表；记录推荐点击行为",
    "分页推荐商品列表（含推荐理由、得分、商品信息）",
    [
      "1. 查询推荐：GET /api/v1/user/recommendations?page=1&size=10",
      "2. 查询user_recommendations表中当前用户的推荐记录，按recommend_score降序排列",
      "3. 刷新推荐：POST /api/v1/user/recommendations/refresh?limit=12",
      "4. 分析user_behavior_logs表中用户行为（浏览、搜索、评论、购买等），提取偏好类目和关键词",
      "5. 按推荐策略（similar_category、recent_view、hot_sale、keyword_match、collaborative_filtering）计算得分",
      "6. 清除旧推荐，写入新推荐到user_recommendations表",
      "7. 记录点击：POST /api/v1/user/recommendations/{id}/click",
      "8. 设置user_recommendations.is_clicked=true",
    ],
    [
      "推荐页面（RecommendationsView.vue）以商品卡片网格形式展示个性化推荐商品，每个卡片显示商品封面、标题、价格和推荐理由标签。页面提供\"换一批\"按钮触发推荐刷新。",
    ]
  ));

  // ── 13 管理-身份认证 ──
  allModules.push(...moduleSection(
    "管理-身份认证", "admin_auth",
    "管理员在管理后台登录页面输入管理员账号、密码和验证码后点击\"登录\"按钮",
    "管理员账号（adminNo）、密码（password）、验证码（captcha）、验证码Key（captchaKey）",
    "验证图形验证码，根据adminNo查询admins表，BCrypt校验密码，检查账户状态和角色，生成JWT令牌",
    "JWT访问令牌（accessToken）、管理员信息（账号、姓名、角色）",
    [
      "1. GET /api/v1/admin/auth/captcha 获取验证码",
      "2. POST /api/v1/admin/auth/login 提交AdminLoginRequest",
      "3. 验证验证码有效性",
      "4. 根据adminNo（格式：admin+数字）查询admins表",
      "5. BCrypt校验密码",
      "6. 检查account_status=active",
      "7. 生成包含adminId、adminNo、role_code的JWT令牌",
      "8. 更新admins.last_login_at",
      "9. 记录登录日志到login_logs表（account_type=admin）",
    ],
    [
      "管理员登录页面（admin/LoginView.vue）提供管理员账号、密码和验证码输入框。登录成功后将admin JWT存入Pinia store，路由跳转至管理后台仪表盘页面。管理后台使用AdminLayout布局，左侧为导航菜单。",
    ]
  ));

  // ── 14 管理-注册审核 ──
  allModules.push(...moduleSection(
    "管理-注册审核", "admin_registration",
    "管理员在注册审核页面查看待审核的注册申请并执行审批/驳回操作",
    "筛选参数（状态、学号、邮箱）；审核意见（reviewRemark）",
    "分页查询注册申请列表；查看申请详情（含学生证图片）；执行审批（创建用户账号）或驳回操作",
    "注册申请列表/详情；审核结果（含生成的用户信息）",
    [
      "1. 查询列表：GET /api/v1/admin/registration-applications?status=pending&page=1&size=10",
      "2. 查看详情：GET /api/v1/admin/registration-applications/{id}",
      "3. 审批通过：POST /api/v1/admin/registration-applications/{id}/approve",
      "4. 后端RegistrationReviewServiceImpl更新申请状态为approved",
      "5. 根据申请信息创建users表记录（复制学号、姓名、性别、邮箱、手机、密码哈希、学院、专业、班级、学生证ID）",
      "6. 设置reviewer_admin_id和reviewed_at",
      "7. 发送站内通知和邮件通知申请人审核结果",
      "8. 记录管理操作日志到admin_operation_logs表",
      "9. 驳回：POST /api/v1/admin/registration-applications/{id}/reject",
      "10. 更新申请状态为rejected，记录驳回原因，发送通知",
    ],
    [
      "注册审核页面（RegistrationsView.vue）以表格形式展示注册申请列表，支持按状态（待审核/已通过/已驳回）和学号/邮箱筛选。详情弹窗展示申请人完整信息和学生证图片，底部提供\"通过\"和\"驳回\"操作按钮及审核意见输入框。",
    ]
  ));

  // ── 15 管理-用户管理 ──
  allModules.push(...moduleSection(
    "管理-用户管理", "admin_user_mgmt",
    "管理员在用户管理页面查看和管理平台用户的账户状态",
    "筛选参数（账户状态、学号、姓名）；用户ID、目标状态、操作备注（更新状态时）",
    "分页查询用户列表；查看用户详情；更新用户账户状态（启用/禁用/锁定）",
    "用户列表/详情；状态更新结果",
    [
      "1. 查询列表：GET /api/v1/admin/users?accountStatus=active&page=1&size=10",
      "2. 后端AdminUserManagementServiceImpl构建查询条件，分页查询users表",
      "3. 查看详情：GET /api/v1/admin/users/{userId}",
      "4. 更新状态：POST /api/v1/admin/users/{userId}/status 提交UpdateUserStatusRequest",
      "5. 更新users.account_status（active/disabled/locked）",
      "6. 记录管理操作日志到admin_operation_logs表",
    ],
    [
      "用户管理页面（UsersView.vue）以表格形式展示用户列表，包含学号、姓名、邮箱、学院、账户状态、注册时间等列。支持按账户状态和学号/姓名搜索筛选。每行提供\"禁用\"或\"启用\"操作按钮和操作备注弹窗。",
    ]
  ));

  // ── 16 管理-商品管理 ──
  allModules.push(...moduleSection(
    "管理-商品管理", "admin_item_mgmt",
    "管理员在商品管理页面审核和管理平台商品",
    "筛选参数（状态、分类ID、关键词、卖家学号）；商品ID、目标状态、操作备注（更新状态时）",
    "分页查询所有商品列表；查看商品详情；更新商品状态（下架/恢复）",
    "商品列表/详情；状态更新结果",
    [
      "1. 查询列表：GET /api/v1/admin/items?status=on_sale&page=1&size=10",
      "2. 支持按status、categoryId、keyword、sellerStudentNo多条件组合筛选",
      "3. 查看详情：GET /api/v1/admin/items/{itemId}",
      "4. 更新状态：POST /api/v1/admin/items/{itemId}/status 提交UpdateItemStatusRequest",
      "5. 可将商品设为off_shelf（强制下架）等状态",
      "6. 记录管理操作日志到admin_operation_logs表",
    ],
    [
      "商品管理页面（admin/ItemsView.vue）以表格展示全平台商品列表，提供多条件搜索面板。每行显示商品标题、卖家学号、分类、价格、状态和发布时间，提供\"下架\"等操作按钮。",
    ]
  ));

  // ── 17 管理-订单管理 ──
  allModules.push(...moduleSection(
    "管理-订单管理", "admin_order_mgmt",
    "管理员在订单管理页面查看和干预异常订单",
    "筛选参数（订单状态、订单号、买家/卖家学号）；订单ID、操作备注（取消/关闭时）",
    "分页查询所有订单列表；查看订单详情；管理员取消或关闭订单",
    "订单列表/详情；操作结果",
    [
      "1. 查询列表：GET /api/v1/admin/orders?orderStatus=pending_confirm&page=1&size=10",
      "2. 支持按orderStatus、orderNo、buyerStudentNo、sellerStudentNo筛选",
      "3. 查看详情：GET /api/v1/admin/orders/{orderId}",
      "4. 管理员取消：POST /api/v1/admin/orders/{orderId}/cancel",
      "5. 设置cancelled_by=admin，恢复商品库存",
      "6. 管理员关闭：POST /api/v1/admin/orders/{orderId}/close，状态→closed",
      "7. 记录管理操作日志，发送通知给买卖双方",
    ],
    [
      "订单管理页面（admin/OrdersView.vue）以表格展示全平台订单，支持多条件筛选。每行显示订单号、买家、卖家、金额、状态和时间，提供\"取消\"和\"关闭\"操作按钮及备注弹窗。",
    ]
  ));

  // ── 18 管理-公告管理 ──
  allModules.push(...moduleSection(
    "管理-公告管理", "admin_announcement",
    "管理员在公告管理页面创建、编辑、发布或下线公告",
    "公告信息（标题、内容、是否置顶、发布状态、过期时间）",
    "创建/更新/发布/下线公告；分页查询公告列表",
    "公告详情或公告列表",
    [
      "1. 查询列表：GET /api/v1/admin/announcements?publishStatus=published&page=1&size=10",
      "2. 创建公告：POST /api/v1/admin/announcements 提交SaveAnnouncementRequest",
      "3. 设置publisher_admin_id为当前管理员，插入announcements表",
      "4. 编辑公告：PUT /api/v1/admin/announcements/{id}",
      "5. 发布：POST /api/v1/admin/announcements/{id}/publish，设置publish_status=published和published_at",
      "6. 下线：POST /api/v1/admin/announcements/{id}/offline，设置publish_status=offline",
      "7. 需要SUPER_ADMIN或OPERATOR角色权限",
    ],
    [
      "公告管理页面（admin/AnnouncementsView.vue）以表格展示公告列表，支持按发布状态筛选。提供\"新建公告\"按钮。编辑弹窗包含标题输入框、富文本内容编辑器、是否置顶开关和过期时间选择器。每行提供编辑、发布、下线操作按钮。",
    ]
  ));

  // ── 19 管理-数据仪表盘 ──
  allModules.push(...moduleSection(
    "管理-数据仪表盘", "admin_dashboard",
    "管理员登录后台后访问仪表盘页面",
    "时间范围参数（天数days）、数量限制（limit）",
    "聚合查询数据库统计信息，生成概览数据、趋势图数据、分类排行、热搜词和用户增长趋势",
    "概览指标（用户总数、商品总数、订单总数、待审核数等）、订单趋势折线图数据、商品状态饼图数据、最近活动列表、分类销售排行、热搜关键词排行、用户增长趋势",
    [
      "1. 概览数据：GET /api/v1/admin/dashboard/overview",
      "2. 聚合查询users、items、orders、registration_applications表的count统计",
      "3. 订单趋势：GET /api/v1/admin/dashboard/order-trends?days=7",
      "4. 按日期分组统计近N天每天的订单数量",
      "5. 商品状态分布：GET /api/v1/admin/dashboard/item-status",
      "6. 按status分组统计各状态商品数量",
      "7. 最近活动：GET /api/v1/admin/dashboard/recent-activities?limit=10",
      "8. 查询最近的注册审核、订单状态变更等操作日志",
      "9. 分类销售排行：GET /api/v1/admin/dashboard/category-sales-ranking?days=30&limit=10",
      "10. 热搜关键词：GET /api/v1/admin/dashboard/hot-search-keywords?days=7&limit=10",
      "11. 用户增长趋势：GET /api/v1/admin/dashboard/user-growth-trends?days=7",
    ],
    [
      "仪表盘页面（DashboardView.vue）顶部展示概览卡片（用户数、商品数、订单数、待审核数等数字指标），中间区域为ECharts图表（订单趋势折线图、商品状态饼图、分类销售排行柱状图、热搜词云），底部为最近活动时间线和用户增长趋势图。",
    ]
  ));

  // ── 20 管理-报表导出 ──
  allModules.push(...moduleSection(
    "管理-报表导出", "admin_report_export",
    "管理员在报表管理页面选择报表类型和时间范围后点击\"导出\"按钮",
    "报表类型（概览/订单趋势/分类排行/热搜词/用户增长）、时间范围（天数或自定义起止日期）、数量限制",
    "根据报表类型和时间范围查询数据，生成CSV格式报表文件，通过HTTP响应下载",
    "CSV文件（Content-Disposition: attachment），包含UTF-8 BOM和对应的统计数据表格",
    [
      "1. 概览报表：GET /api/v1/admin/reports/dashboard-overview.csv",
      "2. 订单趋势：GET /api/v1/admin/reports/order-trends.csv?days=7 或 ?startDate=&endDate=",
      "3. 分类排行：GET /api/v1/admin/reports/category-sales-ranking.csv?days=30&limit=10",
      "4. 热搜词：GET /api/v1/admin/reports/hot-search-keywords.csv?days=7&limit=10",
      "5. 用户增长：GET /api/v1/admin/reports/user-growth-trends.csv?days=7",
      "6. AdminReportExportServiceImpl调用对应的Dashboard方法获取数据",
      "7. 将数据格式化为CSV（UTF-8 with BOM）",
      "8. 设置响应头Content-Type: text/csv、Content-Disposition: attachment; filename=xxx.csv",
      "9. 返回ResponseEntity<byte[]>触发浏览器下载",
    ],
    [
      "报表管理页面（admin/ReportsView.vue）提供报表类型选择卡片，每种报表类型可选择时间范围（快速选择近7天/30天/自定义日期区间）和数量限制。每个报表卡片底部设有\"导出CSV\"按钮，点击后触发浏览器下载。",
    ]
  ));

  return [
    heading1("3. 程序设计说明"),
    headerTbl,
    emptyPara(),
    ...allModules,
  ];
}

// ── Assemble document ───────────────────────────────────────────────────
async function main() {
  const doc = new Document({
    styles: {
      default: {
        document: { run: { font: "SimSun", size: 24 } },
      },
      paragraphStyles: [
        { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: 32, bold: true, font: "SimHei" },
          paragraph: { spacing: { before: 240, after: 240 }, outlineLevel: 0 } },
        { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: 28, bold: true, font: "SimHei" },
          paragraph: { spacing: { before: 180, after: 180 }, outlineLevel: 1 } },
        { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: 24, bold: true, font: "SimHei" },
          paragraph: { spacing: { before: 120, after: 120 }, outlineLevel: 2 } },
      ],
    },
    sections: [{
      properties: {
        page: {
          size: { width: PAGE_W, height: PAGE_H },
          margin: { top: MARGIN, right: MARGIN, bottom: MARGIN, left: MARGIN },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: "校园二手交易系统  详细设计说明书", font: "SimSun", size: 18, color: "888888" })],
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: "第 ", font: "SimSun", size: 18 }), new TextRun({ children: [PageNumber.CURRENT], font: "SimSun", size: 18 }), new TextRun({ text: " 页", font: "SimSun", size: 18 })],
          })],
        }),
      },
      children: [
        ...buildCoverSection(),
        ...buildIntroSection(),
        ...buildStructureSection(),
        ...buildModuleDesignSection(),
      ],
    }],
  });

  const buffer = await Packer.toBuffer(doc);
  const outPath = __dirname + "/校园二手交易系统详细设计说明书.docx";
  fs.writeFileSync(outPath, buffer);
  console.log("Generated:", outPath);
}

main().catch(e => { console.error(e); process.exit(1); });
