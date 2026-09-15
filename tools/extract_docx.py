import zipfile, re, sys, os

docx_path = sys.argv[1] if len(sys.argv) > 1 else 'doc/骑行爱好者服务平台的设计与实现（总文档例子模板）.docx'
with zipfile.ZipFile(docx_path, 'r') as z:
    with z.open('word/document.xml') as f:
        content = f.read().decode('utf-8')
        text = re.sub(r'<[^>]+>', ' ', content)
        text = re.sub(r'\s+', ' ', text).strip()
        out_path = '/tmp/template_text.txt'
        with open(out_path, 'w', encoding='utf-8') as out:
            out.write(text)
        print(f'Written {len(text)} chars to {out_path}')
