# coding=utf-8
import sys
import os
import re

# 1. 接收参数 (新增了 session 参数，总共 7 个参数)
# 参数顺序: 姓名, 类型, 证书号, 等级, 期数, 输出路径, 资源目录
if len(sys.argv) < 8:
    print(f"[Error] 参数不足，需要 7 个参数，实际收到 {len(sys.argv)-1} 个")
    sys.exit(10)

name = sys.argv[1]
teachertype = sys.argv[2]   # 例如：徐州市参培教师
certificateno = sys.argv[3]
rank = sys.argv[4]          # 例如：优秀 / 合格
session_str = sys.argv[5]   # 例如：第八期 / 第8期
output_path = sys.argv[6]
resource_dir = sys.argv[7]

BASE_DIR = os.path.abspath(resource_dir)
# 字体路径 (确保这些文件还在资源目录里)
FONT_SIMLI = os.path.join(BASE_DIR, "SIMLI.TTF")
FONT_TIMES = os.path.join(BASE_DIR, "timesbd.ttf")

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    print("[Error] 缺少 Pillow 库")
    sys.exit(99)

# === 🛠️ 工具函数：中文数字转阿拉伯数字 ===
def parse_session_number(s_str):
    # 1. 如果包含阿拉伯数字 (如 "第8期"), 直接提取
    match = re.search(r'\d+', s_str)
    if match:
        return match.group()
    
    # 2. 如果是纯中文 (如 "第八期")，进行简单映射
    cn_map = {
        '一': '1', '二': '2', '三': '3', '四': '4', '五': '5',
        '六': '6', '七': '7', '八': '8', '九': '9', '十': '10'
    }
    for cn, num in cn_map.items():
        if cn in s_str:
            return num
    
    # 默认返回 "8" 防止报错，或者你可以抛出异常
    print(f"[Warning] 无法解析期数: {s_str}，默认使用 8")
    return "8"

# === 🛠️ 核心逻辑：根据新规则生成文件名 ===
def get_template_name(t_type, t_rank, t_session):
    # 1. 解析期数 (例如 "第八期" -> "8")
    s_num = parse_session_number(t_session)

    # 2. 解析地区 (徐州->xz, 潍坊->wf, 其他->sh)
    if '徐州' in t_type:
        area_code = 'xz'
    elif '潍坊' in t_type:
        area_code = 'wf'
    else:
        area_code = 'sh' # 默认：社会/其他 -> sh

    # 3. 解析等级 (优秀->yx, 合格->hg)
    if '优秀' in t_rank:
        rank_code = 'yx'
    else:
        rank_code = 'hg' # 默认合格

    # 4. 组合文件名 (例如 "8xzyx.png")
    return f"{s_num}{area_code}{rank_code}.png"

def generate():
    # 获取动态模板名
    img_name = get_template_name(teachertype, rank, session_str)
    
    template_path = os.path.join(BASE_DIR, img_name)

    if not os.path.exists(template_path):
        print(f"[Error] 找不到模板图片: {img_name}")
        print(f"[Info] 请确保 {img_name} 已经上传到资源目录: {BASE_DIR}")
        sys.exit(11)

    try:
        back_img = Image.open(template_path)
        obj = ImageDraw.Draw(back_img)
        
        # 字体设置 (根据需要调整大小)
        font_name = ImageFont.truetype(FONT_SIMLI, size=125, encoding="utf-8")
        font_cert = ImageFont.truetype(FONT_TIMES, size=70, encoding="utf-8")
        
        # 坐标设置 (注意：如果不同期数的模板文字位置不同，这里需要写 if/else 判断 img_name)
        # 目前假设所有模板的文字位置都一样，沿用之前的坐标
        
        # 针对 "徐州/潍坊" 这种模板的坐标 (参考之前的代码)
        if 'xz' in img_name or 'wf' in img_name:
            obj.text((680, 1035), str(name), (0, 0, 0), font=font_name, align="center")
            obj.text((780, 1665), str(certificateno), (0, 0, 0), font=font_cert, align="center")
        else:
            # 针对 "sh" (社会人员) 的坐标
            # 如果新模板位置变了，请修改这里的坐标
            obj.text((690, 1292), str(name), (0, 0, 0), font=font_name, align="center")
            obj.text((570, 1695), str(certificateno), (0, 0, 0), font=font_cert, align="center")
        
        # 保存
        back_img.save(output_path)
        print(f"Success: {output_path}")

    except Exception as e:
        print(f"[Error] 生成出错: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(99)

if __name__ == '__main__':
    generate()