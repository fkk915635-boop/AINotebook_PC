import sys
import json
import os
import random
import threading
import requests
from datetime import datetime
from dotenv import load_dotenv
from PyQt5.QtWidgets import (
    QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout,
    QPushButton, QTextEdit, QLabel, QScrollArea, QFrame, QSplitter,
    QFileDialog, QMessageBox, QShortcut
)
from PyQt5.QtCore import Qt, QThread, pyqtSignal
from PyQt5.QtGui import QFont, QColor, QPalette, QIcon
from PyQt5.QtCore import QSize
import qtawesome as qta


class AIWorker(QThread):
    """AI 分析线程（避免界面卡死）"""
    result_signal = pyqtSignal(str)
    error_signal = pyqtSignal(str)

    def __init__(self, api_key, notes):
        super().__init__()
        self.api_key = api_key
        self.notes = notes
        self.use_real_ai = bool(api_key)

    def run(self):
        try:
            if self.use_real_ai:
                result = self.call_deepseek_api()
            else:
                result = self.simulate_ai_analysis()
            self.result_signal.emit(result)
        except Exception as e:
            self.error_signal.emit(f"AI 分析失败: {str(e)[:100]}")

    def call_deepseek_api(self):
        """调用 DeepSeek API（真实 AI）"""
        context = "\n".join(self.notes[:5])
        prompt = f"""
你是一位专业的思维分析师，请基于用户最近的思考记录，生成一份深度洞察报告。

要求：
1. 用中文输出，语气温暖、有启发性
2. 分析维度：
   - 核心主题（1-2 个高频关键词）
   - 情绪倾向（积极/平静/焦虑/困惑等）
   - 思维模式（如：反思型、规划型、创意型）
   - 1 个启发性问题（引导用户深度思考）
3. 格式简洁，避免冗长

用户最近的思考记录：
{context}
        """.strip()

        response = requests.post(
            "https://api.deepseek.com/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json"
            },
            json={
                "model": "deepseek-chat",
                "messages": [{"role": "user", "content": prompt}],
                "temperature": 0.7,
                "max_tokens": 500
            },
            timeout=15
        )

        if response.status_code != 200:
            raise Exception(f"API 错误 {response.status_code}: {response.text[:200]}")

        data = response.json()
        return data["choices"][0]["message"]["content"].strip()

    def simulate_ai_analysis(self):
        """模拟 AI 分析（无网络/无 Key 时降级）"""
        themes = ["自我成长", "情绪管理", "职业规划", "学习效率", "人际关系", "创造力", "时间管理", "目标设定"]
        emotions = {
            "积极": "😊 情绪饱满，充满动力",
            "平静": "😌 内心平和，理性思考",
            "焦虑": "😟 略有压力，需要放松",
            "困惑": "🤔 正在探索，保持好奇",
            "兴奋": "✨ 灵感迸发，创意涌现"
        }

        theme = random.choice(themes)
        emotion = random.choice(list(emotions.values()))
        insight = random.choice([
            "你最近的思考更偏向长期规划",
            "情绪波动中藏着成长契机",
            "重复出现的主题值得深入探索",
            "记录频率反映当前生活节奏"
        ])

        return f"""
🧠 AI 洞察报告（模拟模式）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
核心主题：{theme}
情绪画像：{emotion}
深度洞察：{insight}

💡 行动建议：
• 每周回顾一次笔记，发现思维模式变化
• 对高频主题做专项记录（如「情绪日记」）
• 尝试用语音输入提升记录效率

✨ 提示：配置 DeepSeek API Key 可获得真实 AI 分析
        """.strip()


class AINotebook(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("🧠 AI 智能记事本")
        self.setGeometry(100, 100, 900, 700)
        self.setWindowIcon(QIcon(qta.icon('fa5s.brain', color='white')))

        # 暗黑模式设置
        self.set_dark_theme()

        # 加载 API Key
        load_dotenv()
        self.api_key = os.getenv("DEEPSEEK_API_KEY", "").strip()
        self.use_real_ai = bool(self.api_key)

        # 数据路径
        self.data_dir = "data"
        self.data_file = os.path.join(self.data_dir, "notes.json")
        os.makedirs(self.data_dir, exist_ok=True)

        self.notes = self.load_notes()
        self.init_ui()
        self.display_notes()

    def set_dark_theme(self):
        """设置暗黑模式主题"""
        palette = QPalette()
        palette.setColor(QPalette.Window, QColor(30, 30, 40))
        palette.setColor(QPalette.WindowText, QColor(220, 220, 220))
        palette.setColor(QPalette.Base, QColor(25, 25, 35))
        palette.setColor(QPalette.AlternateBase, QColor(35, 35, 45))
        palette.setColor(QPalette.ToolTipBase, QColor(255, 255, 220))
        palette.setColor(QPalette.ToolTipText, QColor(0, 0, 0))
        palette.setColor(QPalette.Text, QColor(220, 220, 220))
        palette.setColor(QPalette.Button, QColor(40, 40, 50))
        palette.setColor(QPalette.ButtonText, QColor(220, 220, 220))
        palette.setColor(QPalette.Highlight, QColor(70, 130, 180))
        palette.setColor(QPalette.HighlightedText, QColor(255, 255, 255))
        self.setPalette(palette)

        # 字体设置
        font = QFont("Microsoft YaHei", 10)
        self.setFont(font)

    def init_ui(self):
        # 主布局
        central_widget = QWidget()
        main_layout = QHBoxLayout(central_widget)

        # 左侧：输入区域
        left_panel = QWidget()
        left_layout = QVBoxLayout(left_panel)
        left_layout.setContentsMargins(20, 20, 20, 20)
        left_layout.setSpacing(15)

        # 标题
        title = QLabel("🧠 AI 智能记事本")
        title.setFont(QFont("Microsoft YaHei", 18, QFont.Bold))
        title.setStyleSheet("color: #64b5f6;")
        left_layout.addWidget(title)

        # 输入框
        self.input_text = QTextEdit()
        self.input_text.setPlaceholderText("写下你的想法...")
        self.input_text.setStyleSheet("""
            QTextEdit {
                background-color: #353545;
                border: 1px solid #454555;
                border-radius: 8px;
                color: #e0e0e0;
                padding: 10px;
                font-size: 14px;
            }
            QTextEdit:focus {
                border: 1px solid #64b5f6;
            }
        """)
        self.input_text.setMinimumHeight(200)
        left_layout.addWidget(self.input_text)

        # 按钮区域
        btn_layout = QHBoxLayout()
        btn_layout.setSpacing(10)

        self.ai_btn = QPushButton("🤖 AI 整合")
        self.ai_btn.setStyleSheet("""
            QPushButton {
                background-color: #42a5f5;
                color: white;
                border: none;
                border-radius: 8px;
                padding: 10px 20px;
                font-weight: bold;
                font-size: 14px;
            }
            QPushButton:hover {
                background-color: #1e88e5;
            }
            QPushButton:pressed {
                background-color: #0d47a1;
            }
        """)
        self.ai_btn.setIcon(qta.icon('fa5s.robot', color='white', scale_factor=1.2))
        self.ai_btn.setIconSize(QSize(24, 24))
        self.ai_btn.clicked.connect(self.start_ai_analysis)
        btn_layout.addWidget(self.ai_btn)

        self.save_btn = QPushButton("💾 保存 (Ctrl+S)")
        self.save_btn.setStyleSheet("""
            QPushButton {
                background-color: #43a047;
                color: white;
                border: none;
                border-radius: 8px;
                padding: 10px 20px;
                font-weight: bold;
                font-size: 14px;
            }
            QPushButton:hover {
                background-color: #388e3c;
            }
            QPushButton:pressed {
                background-color: #1b5e20;
            }
        """)
        self.save_btn.setIcon(qta.icon('fa5s.save', color='white', scale_factor=1.2))
        self.save_btn.setIconSize(QSize(24, 24))
        self.save_btn.clicked.connect(self.save_note)
        btn_layout.addWidget(self.save_btn)

        left_layout.addLayout(btn_layout)

        # 右侧：笔记列表
        right_panel = QWidget()
        right_layout = QVBoxLayout(right_panel)
        right_layout.setContentsMargins(20, 20, 20, 20)
        right_layout.setSpacing(15)

        # 标题
        notes_title = QLabel("📖 历史记录")
        notes_title.setFont(QFont("Microsoft YaHei", 14, QFont.Bold))
        notes_title.setStyleSheet("color: #a5d6a7;")
        right_layout.addWidget(notes_title)

        # 笔记列表容器
        self.notes_list = QScrollArea()
        self.notes_list.setWidgetResizable(True)
        self.notes_list.setStyleSheet("""
            QScrollArea {
                border: none;
                background-color: #252535;
                border-radius: 8px;
            }
        """)

        notes_container = QWidget()
        self.notes_list_layout = QVBoxLayout(notes_container)
        self.notes_list_layout.setContentsMargins(10, 10, 10, 10)
        self.notes_list_layout.setSpacing(15)
        self.notes_list_layout.addStretch()

        notes_container.setLayout(self.notes_list_layout)
        self.notes_list.setWidget(notes_container)
        right_layout.addWidget(self.notes_list)

        # 状态栏
        self.status_bar = QLabel("✅ 就绪 | " + ("🌐 真实 AI 模式" if self.use_real_ai else "🤖 模拟 AI 模式"))
        self.status_bar.setStyleSheet("color: #a5d6a7; font-size: 12px;")
        right_layout.addWidget(self.status_bar)

        # 添加分割器
        splitter = QSplitter(Qt.Horizontal)
        splitter.addWidget(left_panel)
        splitter.addWidget(right_panel)
        splitter.setSizes([400, 500])

        main_layout.addWidget(splitter)
        self.widget = self.setCentralWidget(central_widget)

        # 快捷键
        self.shortcut_save = QShortcut("Ctrl+S", self)
        self.shortcut_save.activated.connect(self.save_note)

    def save_note(self):
        note = self.input_text.toPlainText().strip()
        if not note:
            QMessageBox.warning(self, "⚠️ 警告", "请输入内容后再保存！")
            return

        timestamp = datetime.now().strftime("%m-%d %H:%M")
        self.notes.insert(0, f"[{timestamp}] {note}")
        self.save_notes()
        self.input_text.clear()
        self.display_notes()
        QMessageBox.information(self, "✅ 保存成功", "想法已存入本地数据库！")
        self.status_bar.setText(f"✅ 已保存 | {timestamp}")

    def display_notes(self):
        # 清空当前笔记列表
        for i in reversed(range(self.notes_list_layout.count())):
            widget = self.notes_list_layout.itemAt(i).widget()
            if widget is not None:
                widget.deleteLater()

        if not self.notes:
            hint = QLabel("📝 暂无想法，快写下第一条吧～")
            hint.setFont(QFont("Microsoft YaHei", 12))
            hint.setStyleSheet("color: #888; padding: 10px;")
            self.notes_list_layout.addWidget(hint)
        else:
            for i, note in enumerate(self.notes[:20], 1):
                note_frame = QFrame()
                note_frame.setStyleSheet("""
                    QFrame {
                        background-color: #353545;
                        border-radius: 8px;
                        padding: 12px;
                        border: 1px solid #454555;
                    }
                """)
                note_layout = QVBoxLayout(note_frame)

                # 标题
                title = QLabel(f"{i}. {note.split(']')[0].strip()}]")
                title.setFont(QFont("Microsoft YaHei", 10, QFont.Bold))
                title.setStyleSheet("color: #64b5f6;")
                note_layout.addWidget(title)

                # 内容
                content = QLabel(note.split(']', 1)[1].strip())
                content.setFont(QFont("Microsoft YaHei", 10))
                content.setWordWrap(True)
                content.setStyleSheet("color: #e0e0e0; margin-top: 5px;")
                note_layout.addWidget(content)

                self.notes_list_layout.addWidget(note_frame)

        # 刷新布局
        self.notes_list_layout.addStretch()
        self.notes_list_layout.update()

    def start_ai_analysis(self):
        if not self.notes:
            QMessageBox.warning(self, "⚠️ 警告", "请先记录至少一条想法！")
            return

        # 禁用按钮
        self.ai_btn.setEnabled(False)
        self.status_bar.setText("⏳ 正在分析思维模式...")

        # 启动 AI 线程
        self.worker = AIWorker(self.api_key, self.notes)
        self.worker.result_signal.connect(self.show_ai_result)
        self.worker.error_signal.connect(self.show_ai_error)
        self.worker.start()

    def show_ai_result(self, content):
        # 弹出分析窗口
        result_win = QMainWindow(self)
        result_win.setWindowTitle("🤖 AI 整合结果")
        result_win.setGeometry(300, 300, 600, 450)
        result_win.setWindowIcon(QIcon(qta.icon('fa5s.robot', color='white')))

        # 内容
        content_widget = QWidget()
        content_layout = QVBoxLayout(content_widget)
        content_layout.setContentsMargins(20, 20, 20, 20)

        # 标题
        title = QLabel("🧠 深度思维分析")
        title.setFont(QFont("Microsoft YaHei", 16, QFont.Bold))
        title.setStyleSheet("color: #64b5f6;")
        content_layout.addWidget(title)

        # 分析内容
        analysis = QTextEdit()
        analysis.setReadOnly(True)
        analysis.setText(content)
        analysis.setStyleSheet("""
            QTextEdit {
                background-color: #252535;
                border: none;
                border-radius: 8px;
                color: #e0e0e0;
                font-size: 14px;
                padding: 15px;
            }
        """)
        content_layout.addWidget(analysis)

        # 关闭按钮
        btn_layout = QHBoxLayout()
        close_btn = QPushButton("✅ 明白了")
        close_btn.setStyleSheet("""
            QPushButton {
                background-color: #43a047;
                color: white;
                border: none;
                border-radius: 8px;
                padding: 10px 20px;
                font-weight: bold;
            }
            QPushButton:hover {
                background-color: #388e3c;
            }
        """)
        close_btn.clicked.connect(result_win.close)
        btn_layout.addStretch()
        btn_layout.addWidget(close_btn)
        content_layout.addLayout(btn_layout)

        content_widget.setLayout(content_layout)
        result_win.setCentralWidget(content_widget)

        # 显示
        result_win.show()
        self.status_bar.setText("✅ AI 分析完成")
        self.ai_btn.setEnabled(True)

    def show_ai_error(self, error_msg):
        QMessageBox.critical(self, "❌ AI 分析失败", error_msg)
        self.status_bar.setText(f"⚠️ {error_msg}")
        self.ai_btn.setEnabled(True)

    def load_notes(self):
        if not os.path.exists(self.data_file):
            return []
        try:
            with open(self.data_file, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"⚠️ 加载笔记失败: {e}")
            return []

    def save_notes(self):
        try:
            with open(self.data_file, "w", encoding="utf-8") as f:
                json.dump(self.notes, f, ensure_ascii=False, indent=2)
        except Exception as e:
            QMessageBox.critical(self, "❌ 保存失败", f"无法写入文件:\n{e}")


if __name__ == "__main__":
    app = QApplication(sys.argv)

    # 设置全局样式
    app.setStyle("Fusion")

    # 设置高 DPI 支持
    app.setAttribute(Qt.AA_EnableHighDpiScaling)
    app.setAttribute(Qt.AA_UseHighDpiPixmaps)

    window = AINotebook()
    window.show()
    sys.exit(app.exec_())