# coding=utf-8
import sys
import os

# 1. 接收参数
if len(sys.argv) < 7:
    print(f"[Error] 参数不足")
    sys.exit(10)

name = sys.argv[1]
teachertype = sys.argv[2]
certificateno = sys.argv[3]
rank = sys.argv[4]
output_path = sys.argv[5]
resource_dir = sys.argv[6]

BASE_DIR = os.path.abspath(resource_dir)
FONT_SIMLI = os.path.join(BASE_DIR, "SIMLI.TTF")
FONT_TIMES = os.path.join(BASE_DIR, "timesbd.ttf")

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    print("[Error] 缺少 Pillow 库")
    sys.exit(99)

# 🔥 核心修改：这里改回中文文件名匹配
def get_template_name(t_type, t_rank):
    if t_type == '潍坊市参培教师':
        return f"潍坊-{t_rank}.png"  # 对应：潍坊-优秀.png
    elif t_type == '徐州市参培教师':
        return f"徐州-{t_rank}.png"
    elif t_type == '其他人员':
        # 请确认你的文件名是 "社会人员证书-优秀.png"
        return f"社会人员证书-{t_rank}.png"
    return None

def generate():
    img_name = get_template_name(teachertype, rank)
    if not img_name:
        print(f"[Error] 未知类型: {teachertype}")
        sys.exit(10)
    
    template_path = os.path.join(BASE_DIR, img_name)

    if not os.path.exists(template_path):
        # 打印出到底在找什么文件
        print(f"[Error] 找不到模板: {img_name}")
        print(f"[Info] 搜索目录: {BASE_DIR}")
        sys.exit(11)

    try:
        back_img = Image.open(template_path)
        obj = ImageDraw.Draw(back_img)
        
        font1 = ImageFont.truetype(FONT_SIMLI, size=125, encoding="utf-8")
        font11 = ImageFont.truetype(FONT_TIMES, size=70, encoding="utf-8")
        font2 = ImageFont.truetype(FONT_SIMLI, size=105, encoding="utf-8")
        font22 = ImageFont.truetype(FONT_TIMES, size=60, encoding="utf-8")

        if teachertype == '潍坊市参培教师' or teachertype == '徐州市参培教师':
            obj.text((680, 1035), str(name), (0, 0, 0), font=font1, align="center")
            obj.text((780, 1665), str(certificateno), (0, 0, 0), font=font11, align="center")
        elif teachertype == '其他人员':
            obj.text((690, 1292), str(name), (0, 0, 0), font=font2, align="center")
            obj.text((570, 1695), str(certificateno), (0, 0, 0), font=font22, align="center")
        
        back_img.save(output_path)
        print(f"Success: {output_path}")

    except Exception as e:
        print(f"[Error] 生成出错: {e}")
        sys.exit(99)

if __name__ == '__main__':
    generate()