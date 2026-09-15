#!/usr/bin/env python3
"""
Generate a standard-formatted thesis .docx from the markdown source.
Uses python-docx for full control over formatting.
"""
import re
import sys
from docx import Document
from docx.shared import Pt, Inches, Cm, RGBColor, Emu
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.section import WD_ORIENT
from docx.oxml.ns import qn, nsdecls
from docx.oxml import parse_xml
import os

SRC = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   'doc', '校园二手交易系统的设计与实现.md')
DST = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   'doc', '校园二手交易系统的设计与实现.docx')

def read_md(path):
    with open(path, 'r', encoding='utf-8') as f:
        return f.read()

def set_cell_border(cell, **kwargs):
    """Set cell border properties."""
    tc = cell._tc
    tcPr = tc.get_or_add_tcPr()
    tcBorders = parse_xml(f'<w:tcBorders {nsdecls("w")}></w:tcBorders>')
    for edge, val in kwargs.items():
        element = parse_xml(
            f'<w:{edge} {nsdecls("w")} w:val="{val.get("val", "single")}" '
            f'w:sz="{val.get("sz", 4)}" w:space="0" w:color="{val.get("color", "000000")}"/>'
        )
        tcBorders.append(element)
    tcPr.append(tcBorders)

def set_run_font(run, name_cn='宋体', name_en='Times New Roman', size=Pt(12), bold=False):
    """Set font for a run with both Chinese and English font names."""
    run.font.size = size
    run.font.bold = bold
    run.font.name = name_en
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.find(qn('w:rFonts'))
    if rFonts is None:
        rFonts = parse_xml(f'<w:rFonts {nsdecls("w")}/>')
        rPr.insert(0, rFonts)
    rFonts.set(qn('w:eastAsia'), name_cn)
    rFonts.set(qn('w:ascii'), name_en)
    rFonts.set(qn('w:hAnsi'), name_en)

def add_paragraph_with_font(doc, text, style=None, name_cn='宋体', name_en='Times New Roman',
                             size=Pt(12), bold=False, alignment=None, spacing_before=None,
                             spacing_after=None, first_line_indent=None):
    """Add a paragraph with proper Chinese font settings."""
    p = doc.add_paragraph(style=style)
    if alignment is not None:
        p.alignment = alignment
    pf = p.paragraph_format
    if spacing_before is not None:
        pf.space_before = spacing_before
    if spacing_after is not None:
        pf.space_after = spacing_after
    if first_line_indent is not None:
        pf.first_line_indent = first_line_indent
    pf.line_spacing = 1.5

    run = p.add_run(text)
    set_run_font(run, name_cn, name_en, size, bold)
    return p

def add_heading_styled(doc, text, level=1):
    """Add a chapter/section heading with proper formatting."""
    if level == 1:  # Chapter title: 第X章
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        pf = p.paragraph_format
        pf.space_before = Pt(24)
        pf.space_after = Pt(12)
        pf.line_spacing = 1.5
        run = p.add_run(text)
        set_run_font(run, '黑体', 'Times New Roman', Pt(16), bold=True)
        return p
    elif level == 2:  # Section: X.X
        p = doc.add_paragraph()
        pf = p.paragraph_format
        pf.space_before = Pt(18)
        pf.space_after = Pt(6)
        pf.line_spacing = 1.5
        run = p.add_run(text)
        set_run_font(run, '黑体', 'Times New Roman', Pt(14), bold=True)
        return p
    elif level == 3:  # Subsection: X.X.X
        p = doc.add_paragraph()
        pf = p.paragraph_format
        pf.space_before = Pt(12)
        pf.space_after = Pt(6)
        pf.line_spacing = 1.5
        run = p.add_run(text)
        set_run_font(run, '黑体', 'Times New Roman', Pt(13), bold=True)
        return p
    else:  # Sub-subsection
        p = doc.add_paragraph()
        pf = p.paragraph_format
        pf.space_before = Pt(6)
        pf.space_after = Pt(3)
        pf.line_spacing = 1.5
        run = p.add_run(text)
        set_run_font(run, '黑体', 'Times New Roman', Pt(12), bold=True)
        return p

def add_body_text(doc, text):
    """Add body text paragraph with first-line indent."""
    return add_paragraph_with_font(doc, text, name_cn='宋体', name_en='Times New Roman',
                                   size=Pt(12), first_line_indent=Cm(0.74),
                                   spacing_before=Pt(0), spacing_after=Pt(0))

def add_cover_page(doc):
    """Create the thesis cover page."""
    # Empty lines for spacing from top
    for _ in range(3):
        p = doc.add_paragraph()
        p.paragraph_format.line_spacing = 1.5

    # Warning text in red
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run('模板不要外传网上，请大家遵照')
    set_run_font(run, '宋体', 'Times New Roman', Pt(24), bold=True)
    run.font.color.rgb = RGBColor(0xFF, 0x00, 0x00)

    for _ in range(4):
        p = doc.add_paragraph()
        p.paragraph_format.line_spacing = 1.5

    # Title
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run('校园二手交易系统的设计与实现')
    set_run_font(run, '黑体', 'Times New Roman', Pt(22), bold=True)

    for _ in range(6):
        doc.add_paragraph()

    # Info table
    info_items = [
        ('题    目：', '校园二手交易系统的设计与实现'),
        ('学    生：', '（填写姓名）'),
        ('院    系：', '计算机与数据科学学院'),
        ('指导老师：', '（填写指导教师）'),
        ('专    业：', '计算机科学与技术'),
        ('班    级：', '（填写班级）'),
    ]

    table = doc.add_table(rows=len(info_items), cols=2)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER

    for i, (label, value) in enumerate(info_items):
        row = table.rows[i]
        # Label cell
        cell0 = row.cells[0]
        cell0.width = Cm(3)
        p0 = cell0.paragraphs[0]
        p0.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        run0 = p0.add_run(label)
        set_run_font(run0, '宋体', 'Times New Roman', Pt(16))
        # Value cell
        cell1 = row.cells[1]
        cell1.width = Cm(8)
        p1 = cell1.paragraphs[0]
        p1.alignment = WD_ALIGN_PARAGRAPH.LEFT
        run1 = p1.add_run(value)
        set_run_font(run1, '宋体', 'Times New Roman', Pt(16))
        # Bottom border for value cell
        set_cell_border(cell1, bottom={"val": "single", "sz": 4, "color": "000000"})

    # Remove table borders
    for row in table.rows:
        for cell in row.cells:
            for paragraph in cell.paragraphs:
                for run in paragraph.runs:
                    pass  # keep runs

    # Page break after cover
    doc.add_page_break()

def add_abstract_pages(doc):
    """Add Chinese and English abstract pages."""
    # Chinese title
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run('校园二手交易系统的设计与实现')
    set_run_font(run, '黑体', 'Times New Roman', Pt(18), bold=True)

    # Chinese abstract heading
    add_heading_styled(doc, '摘    要', 2)

    # Chinese abstract body
    ch_abstract = (
        '近年来，随着高校学生规模的不断扩大和绿色环保理念的深入人心，校园二手物品交易需求日益增长。'
        '大学生群体对于教材、数码设备、体育用品、宿舍用品等二手物品的买卖、交换需求十分旺盛。然而，'
        '传统的校园二手交易方式主要依赖于线下摆摊、QQ群、微信群等渠道，存在信息分散、交易效率低、'
        '商品质量难以保证、缺乏有效监管等问题。为了满足这一不断增长的市场需求，开发一款基于Spring Boot'
        '的校园二手交易系统显得尤为重要。'
    )
    add_body_text(doc, ch_abstract)

    ch_abstract2 = (
        '本项目具有两个核心角色。用户端的主要功能模块包括注册申请与身份认证、个人中心管理、商品发布与管理、'
        '商品浏览与搜索、商品评论互动、求购信息发布、在线下单与订单管理、消息通知中心以及个性化推荐；'
        '管理员端的主要功能模块包括仪表盘数据概览、注册申请审核、用户管理、商品管理、订单管理、公告管理、'
        '数据报表导出、演示模式管理以及SMTP邮件配置。管理员通过全面管理用户和商品信息来确保平台信息的准确性'
        '和交易的安全性。'
    )
    add_body_text(doc, ch_abstract2)

    ch_abstract3 = (
        '本项目前端采用Vue.js 3框架和Element Plus组件库，结合Vite构建工具、TypeScript语言、Pinia状态管理、'
        'Vue Router路由管理、Axios HTTP客户端以及ECharts可视化图表库进行前端页面开发；后端采用Spring Boot 3.3'
        '框架结合MyBatis-Plus ORM框架，数据库选择MySQL 8.0，采用JWT（JSON Web Token）实现无状态身份认证，'
        '使用Spring Security进行权限控制，通过BCrypt加密算法保障用户密码安全。项目经需求分析、系统设计、编码开发、'
        '单元测试与集成测试，最终得到实现，确保平台提供便捷、安全和高效的服务。'
    )
    add_body_text(doc, ch_abstract3)

    # Keywords
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.first_line_indent = Cm(0.74)
    pf.line_spacing = 1.5
    pf.space_before = Pt(12)
    run = p.add_run('关键词：校园二手交易；Spring Boot；Vue.js；MyBatis-Plus；电子商务平台')
    set_run_font(run, '宋体', 'Times New Roman', Pt(12), bold=True)

    # Page break before English
    doc.add_page_break()

    # English abstract
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run('Design and Implementation of Campus Second-hand Trading System')
    set_run_font(run, 'Times New Roman', 'Times New Roman', Pt(18), bold=True)

    add_heading_styled(doc, 'Abstract', 2)

    en_abstract = (
        'In recent years, with the continuous expansion of university student populations and the '
        'growing awareness of green environmental protection, the demand for second-hand item trading '
        'on campus has been increasing. College students have a strong demand for buying, selling, and '
        'exchanging second-hand items such as textbooks, digital devices, sports goods, and dormitory '
        'supplies. However, traditional campus second-hand trading methods mainly rely on offline stalls, '
        'QQ groups, WeChat groups, and other channels, which suffer from problems such as fragmented '
        'information, low transaction efficiency, difficulty in ensuring product quality, and lack of '
        'effective supervision. To meet this growing market demand, it is particularly important to '
        'develop a campus second-hand trading system based on Spring Boot.'
    )
    add_body_text(doc, en_abstract)

    en_abstract2 = (
        'This project has two core roles. The main functional modules on the user side include '
        'registration application and identity authentication, personal center management, item publishing '
        'and management, item browsing and searching, item comment interaction, wanted post publishing, '
        'online ordering and order management, notification center, and personalized recommendations. '
        'The main functional modules on the admin side include dashboard data overview, registration '
        'application review, user management, item management, order management, announcement management, '
        'data report export, demo mode management, and SMTP email configuration.'
    )
    add_body_text(doc, en_abstract2)

    en_abstract3 = (
        'The front-end of this project adopts the Vue.js 3 framework and Element Plus component library, '
        'combined with the Vite build tool, TypeScript language, Pinia state management, Vue Router, '
        'Axios HTTP client, and ECharts visualization chart library. The back-end adopts the Spring Boot '
        '3.3 framework combined with the MyBatis-Plus ORM framework, with MySQL 8.0 as the database, '
        'using JWT for stateless identity authentication, Spring Security for permission control, and '
        'BCrypt encryption algorithm to ensure user password security. After requirements analysis, '
        'system design, coding development, unit testing, and integration testing, the project has been '
        'implemented, ensuring that the platform provides convenient, secure, and efficient services.'
    )
    add_body_text(doc, en_abstract3)

    # English keywords
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.first_line_indent = Cm(0.74)
    pf.line_spacing = 1.5
    pf.space_before = Pt(12)
    run = p.add_run('Keywords: Campus Second-hand Trading; Spring Boot; Vue.js; MyBatis-Plus; E-commerce Platform')
    set_run_font(run, 'Times New Roman', 'Times New Roman', Pt(12), bold=True)

    doc.add_page_break()


def parse_markdown_to_docx(md_text, doc):
    """Parse markdown and add content to the docx document."""
    lines = md_text.split('\n')
    i = 0
    in_table = False
    table_rows = []
    in_code_block = False
    list_buffer = []

    while i < len(lines):
        line = lines[i]

        # Skip empty lines
        if not line.strip():
            if list_buffer:
                for item in list_buffer:
                    add_body_text(doc, '• ' + item)
                list_buffer = []
            if in_table:
                add_table_to_docx(doc, table_rows)
                table_rows = []
                in_table = False
                doc.add_paragraph()  # spacing after table
            i += 1
            continue

        # Handle code blocks
        if line.strip().startswith('```'):
            in_code_block = not in_code_block
            i += 1
            continue
        if in_code_block:
            i += 1
            continue

        # Handle tables
        if '|' in line and line.strip().startswith('|'):
            if not in_table:
                in_table = True
                table_rows = []
            # Skip separator rows like |---|---|
            if re.match(r'^\|[\s\-:|]+\|$', line.strip()):
                i += 1
                continue
            # Parse table row
            cells = [c.strip() for c in line.strip().split('|')[1:-1]]
            table_rows.append(cells)
            i += 1
            continue

        # Handle lists
        if re.match(r'^\d+\.\s+\*\*', line.strip()):
            if list_buffer:
                for item in list_buffer:
                    add_body_text(doc, '• ' + item)
                list_buffer = []
            text = re.sub(r'^\d+\.\s+', '', line.strip())
            text = re.sub(r'\*\*([^*]+)\*\*', r'\1', text)
            add_body_text(doc, text)
            i += 1
            continue

        if re.match(r'^[-•]\s+', line.strip()):
            text = re.sub(r'^[-•]\s+', '', line.strip())
            list_buffer.append(text)
            i += 1
            continue

        if re.match(r'^\d+\.\s+', line.strip()) and not line.strip().startswith('1. '):
            text = line.strip()
            list_buffer.append(text)
            i += 1
            continue

        # Handle headings
        # Chapter headings: ## X XXX (no leading # for first level in md but we use ## for chapters)
        heading_match = re.match(r'^##\s+(\d+)\s+(.+)$', line.strip())
        if heading_match:
            if list_buffer:
                for item in list_buffer:
                    add_body_text(doc, '• ' + item)
                list_buffer = []
            num = heading_match.group(1)
            title = heading_match.group(2)
            full_title = f'第{num}章  {title}'
            add_heading_styled(doc, full_title, 1)
            i += 1
            continue

        # Section headings: ### X.X XXX
        sec_match = re.match(r'^###\s+(\d+\.\d+)\s+(.+)$', line.strip())
        if sec_match:
            if list_buffer:
                for item in list_buffer:
                    add_body_text(doc, '• ' + item)
                list_buffer = []
            full_title = f'{sec_match.group(1)}  {sec_match.group(2)}'
            add_heading_styled(doc, full_title, 2)
            i += 1
            continue

        # Subsection headings: #### X.X.X XXX
        subsec_match = re.match(r'^####\s+(\d+\.\d+\.\d+)\s+(.+)$', line.strip())
        if subsec_match:
            if list_buffer:
                for item in list_buffer:
                    add_body_text(doc, '• ' + item)
                list_buffer = []
            full_title = f'{subsec_match.group(1)}  {subsec_match.group(2)}'
            add_heading_styled(doc, full_title, 3)
            i += 1
            continue

        # Sub-subsection: #### X.X.X.X XXX
        subsub_match = re.match(r'^####\s+(\d+\.\d+\.\d+\.\d+)\s+(.+)$', line.strip())
        if subsub_match:
            if list_buffer:
                for item in list_buffer:
                    add_body_text(doc, '• ' + item)
                list_buffer = []
            full_title = f'{subsec_match.group(1)}  {subsec_match.group(2)}'
            add_heading_styled(doc, full_title, 4)
            i += 1
            continue

        # Horizontal rules (skip)
        if line.strip() == '---':
            if list_buffer:
                for item in list_buffer:
                    add_body_text(doc, '• ' + item)
                list_buffer = []
            i += 1
            continue

        # Bold headings within text: **XXX：** or **XXX:**
        bold_heading = re.match(r'^\*\*(.+?)[：:]\*\*\s*(.*)$', line.strip())
        if bold_heading:
            if list_buffer:
                for item in list_buffer:
                    add_body_text(doc, '• ' + item)
                list_buffer = []
            # Add as a sub-subsection
            add_heading_styled(doc, bold_heading.group(1), 4)
            if bold_heading.group(2):
                add_body_text(doc, bold_heading.group(2))
            i += 1
            continue

        # Regular text - clean up markdown formatting
        text = line.strip()
        # Remove bold markers
        text = re.sub(r'\*\*([^*]+)\*\*', r'\1', text)
        # Remove italic markers
        text = re.sub(r'\*([^*]+)\*', r'\1', text)
        # Remove inline code
        text = re.sub(r'`([^`]+)`', r'\1', text)
        # Remove HTML tags
        text = re.sub(r'<[^>]+>', '', text)

        if text:
            add_body_text(doc, text)

        i += 1

    # Flush remaining list buffer
    if list_buffer:
        for item in list_buffer:
            add_body_text(doc, '• ' + item)


def add_table_to_docx(doc, rows):
    """Add a formatted table to the docx."""
    if not rows:
        return

    # Clean markdown formatting from cells
    clean_rows = []
    for row in rows:
        clean_row = [re.sub(r'\*\*([^*]+)\*\*', r'\1', c) for c in row]
        clean_row = [re.sub(r'<br>', '\n', c) for c in clean_row]
        clean_rows.append(clean_row)

    table = doc.add_table(rows=len(clean_rows), cols=len(clean_rows[0]))
    table.style = 'Table Grid'
    table.alignment = WD_TABLE_ALIGNMENT.CENTER

    for r, row_data in enumerate(clean_rows):
        row = table.rows[r]
        for c, cell_text in enumerate(row_data):
            cell = row.cells[c]
            # Clear default paragraph
            cell.paragraphs[0].clear()
            p = cell.paragraphs[0]
            pf = p.paragraph_format
            pf.line_spacing = 1.15
            pf.space_before = Pt(2)
            pf.space_after = Pt(2)

            run = p.add_run(cell_text)
            if r == 0:
                # Header row
                set_run_font(run, '黑体', 'Times New Roman', Pt(9), bold=True)
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                # Header background
                shading = parse_xml(f'<w:shd {nsdecls("w")} w:fill="D9E2F3" w:val="clear"/>')
                cell._tc.get_or_add_tcPr().append(shading)
            else:
                set_run_font(run, '宋体', 'Times New Roman', Pt(9))

    # Set column widths
    for row in table.rows:
        for cell in row.cells:
            cell.width = Cm(2.5)


def setup_page(doc):
    """Set up page dimensions and margins for thesis format."""
    for section in doc.sections:
        section.page_width = Cm(21.0)   # A4 width
        section.page_height = Cm(29.7)  # A4 height
        section.top_margin = Cm(2.54)
        section.bottom_margin = Cm(2.54)
        section.left_margin = Cm(3.18)
        section.right_margin = Cm(3.18)


def main():
    print(f'Reading markdown from: {SRC}')
    md_text = read_md(SRC)

    doc = Document()

    # Set default styles
    style = doc.styles['Normal']
    style.font.name = 'Times New Roman'
    style.font.size = Pt(12)
    style.element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')

    # Setup page
    setup_page(doc)

    # Add cover page
    add_cover_page(doc)

    # Add abstract pages
    add_abstract_pages(doc)

    # Add table of contents placeholder
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    pf = p.paragraph_format
    pf.space_before = Pt(24)
    pf.space_after = Pt(12)
    run = p.add_run('目    录')
    set_run_font(run, '黑体', 'Times New Roman', Pt(16), bold=True)
    doc.add_paragraph('（请在Word中插入自动目录：引用 → 目录 → 自动目录）')
    doc.add_page_break()

    # Parse and add the main content
    # Find the start of chapter 1
    start_idx = md_text.find('\n## 1 前言')
    if start_idx == -1:
        start_idx = md_text.find('\n# 1 前言')
    if start_idx == -1:
        print('ERROR: Could not find Chapter 1 start')
        sys.exit(1)

    # Find where the References start and stop before them
    ref_idx = md_text.find('\n## 参考文献')
    if ref_idx == -1:
        ref_idx = len(md_text)

    main_text = md_text[start_idx:ref_idx]

    print(f'Parsing main content ({len(main_text)} chars)...')
    parse_markdown_to_docx(main_text, doc)

    # Add References
    doc.add_page_break()
    add_heading_styled(doc, '参考文献', 1)
    ref_text = md_text[ref_idx:]
    # Find acknowledgements
    ack_idx = ref_text.find('\n## 致谢')
    if ack_idx != -1:
        ref_only = ref_text[:ack_idx]
        ack_only = ref_text[ack_idx:]
    else:
        ref_only = ref_text
        ack_only = ''

    # Parse references
    ref_lines = ref_only.split('\n')
    for line in ref_lines:
        line = line.strip()
        if line and not line.startswith('#') and not line.startswith('---') and line != '参考文献':
            # Clean markdown
            text = re.sub(r'\*\*([^*]+)\*\*', r'\1', line)
            text = re.sub(r'\[([^\]]+)\]\([^)]+\)', r'\1', text)
            if text and text != '参考文献':
                add_body_text(doc, text)

    # Add Acknowledgements
    if ack_only:
        doc.add_page_break()
        add_heading_styled(doc, '致    谢', 1)
        ack_lines = ack_only.split('\n')
        for line in ack_lines:
            line = line.strip()
            if line and not line.startswith('#') and not line.startswith('---') and line != '致谢':
                text = re.sub(r'\*\*([^*]+)\*\*', r'\1', line)
                if text and text != '致谢':
                    add_body_text(doc, text)

    print(f'Saving to: {DST}')
    doc.save(DST)
    print(f'Done! File size: {os.path.getsize(DST):,} bytes')


if __name__ == '__main__':
    main()
