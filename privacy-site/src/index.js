const lastUpdated = "2026-05-09";

const html = String.raw`<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <meta name="robots" content="index,follow" />
  <title>HRT Recorder 隐私政策与用户协议 / Privacy Policy and Terms</title>
  <style>
    :root {
      color-scheme: light;
      --ink: #111827;
      --muted: #667085;
      --line: #e7edf5;
      --blue: #79cdf4;
      --pink: #ec8fbd;
      --rose: #d94f98;
      --card: rgba(255,255,255,.86);
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans SC", "Microsoft YaHei", sans-serif;
      line-height: 1.72;
      color: var(--ink);
      background:
        radial-gradient(circle at 0% 0%, rgba(121,205,244,.55), transparent 34rem),
        radial-gradient(circle at 100% 8%, rgba(236,143,189,.48), transparent 34rem),
        linear-gradient(180deg, #f8fcff 0%, #fff5fa 100%);
    }
    header, main, footer { width: min(980px, calc(100% - 32px)); margin: 0 auto; }
    header { padding: 52px 0 18px; }
    .hero, section {
      background: var(--card);
      border: 1px solid rgba(255,255,255,.94);
      border-radius: 28px;
      box-shadow: 0 22px 60px rgba(86,119,156,.14);
      padding: 28px;
      margin: 18px 0;
      backdrop-filter: blur(12px);
    }
    h1 { margin: 0 0 10px; font-size: clamp(2.1rem, 5vw, 4.4rem); line-height: 1.05; letter-spacing: -.05em; }
    h2 { margin: 0 0 14px; font-size: 1.55rem; }
    h3 { margin: 22px 0 8px; font-size: 1.12rem; }
    p, li { color: var(--muted); }
    strong { color: var(--ink); }
    a { color: #1976c9; font-weight: 700; text-decoration: none; }
    a:hover { text-decoration: underline; }
    .badge {
      display: inline-flex;
      gap: 8px;
      align-items: center;
      color: #994168;
      background: rgba(236,143,189,.16);
      border: 1px solid rgba(236,143,189,.28);
      padding: 8px 12px;
      border-radius: 999px;
      font-weight: 800;
    }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 14px; }
    .mini {
      border: 1px solid var(--line);
      border-radius: 20px;
      padding: 14px 16px;
      background: rgba(255,255,255,.74);
    }
    .nav { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 18px; }
    .nav a { color: var(--ink); background: #fff; border: 1px solid var(--line); padding: 8px 12px; border-radius: 999px; }
    table { width: 100%; border-collapse: collapse; overflow: hidden; border-radius: 18px; }
    th, td { text-align: left; vertical-align: top; border-bottom: 1px solid var(--line); padding: 12px; }
    th { background: rgba(121,205,244,.12); color: var(--ink); }
    code { background: #f3f7fb; padding: 2px 6px; border-radius: 8px; }
    .warning { border-color: rgba(217,79,152,.32); background: rgba(236,143,189,.10); }
    footer { padding: 24px 0 44px; color: var(--muted); }
  </style>
</head>
<body>
  <header>
    <div class="hero">
      <span class="badge">HRT Recorder · Privacy Policy</span>
      <h1>隐私政策与用户协议</h1>
      <p><strong>应用名称：</strong>HRT Recorder　<strong>包名：</strong><code>com.nanxin.hrtrecorder</code></p>
      <p><strong>开发者 / 作者：</strong>南盺　<strong>隐私联系邮箱：</strong><a href="mailto:wangyanluo233@gmail.com">wangyanluo233@gmail.com</a></p>
      <p><strong>生效与最后更新日期：</strong>${lastUpdated}</p>
      <p>本页面用于 Google Play Console 隐私政策链接、应用内公开展示和用户查阅。HRT Recorder 是一个本地离线记录与趋势估算工具，默认不申请网络权限，不接入登录、广告、统计 SDK、云同步或远程配置。</p>
      <div class="nav">
        <a href="#privacy">隐私政策</a>
        <a href="#data">数据与设备信息</a>
        <a href="#terms">用户协议</a>
        <a href="#medical">健康免责声明</a>
        <a href="#english">English Summary</a>
      </div>
    </div>
  </header>

  <main>
    <section id="privacy">
      <h2>一、隐私政策摘要</h2>
      <div class="grid">
        <div class="mini"><strong>不收集</strong><br />开发者不会从应用中收集、上传、同步或出售用户数据。</div>
        <div class="mini"><strong>本地保存</strong><br />用药、化验、体重、围度、计划、药瓶、设置等数据保存在用户设备本地。</div>
        <div class="mini"><strong>无网络权限</strong><br />应用不声明 <code>INTERNET</code> 权限。离线状态下可完整使用核心功能。</div>
        <div class="mini"><strong>用户主动导出/分享</strong><br />只有用户主动导出、截图、保存图片、分享图片或选择系统日历时，数据才会离开应用界面。</div>
      </div>
    </section>

    <section id="data">
      <h2>二、应用内数据与设备数据如何使用</h2>
      <p>Google Play 要求隐私政策说明应用如何访问、收集、使用和分享用户数据。HRT Recorder 对数据的处理方式如下：</p>
      <table>
        <thead>
          <tr><th>数据或设备能力</th><th>用途</th><th>是否上传给开发者</th><th>是否分享给第三方</th></tr>
        </thead>
        <tbody>
          <tr>
            <td>用药记录、药物计划、药瓶库存、服用时间、剂量、给药方式</td>
            <td>生成本地记录、趋势曲线、提醒状态和库存扣减。</td>
            <td>否。仅保存在本机。</td>
            <td>否。除非用户主动导出或分享给其它应用。</td>
          </tr>
          <tr>
            <td>化验结果、体重、围度、罩杯计算输入、校准模型选择</td>
            <td>用于本地趋势估算、曲线校准、参考区间提示和页面展示。</td>
            <td>否。开发者不可访问。</td>
            <td>否。用户主动导出/分享后由用户和接收方管理。</td>
          </tr>
          <tr>
            <td>JSON / CSV / 图片导入导出文件</td>
            <td>通过 Android 系统文件选择器读取用户选中的文件，或写入用户选择的位置。</td>
            <td>否。</td>
            <td>否。文件保存位置和后续传递由用户决定。</td>
          </tr>
          <tr>
            <td>分享图片与分享文案</td>
            <td>用户点击分享时，应用临时生成 PNG，并通过 Android 分享面板或 X / Twitter 客户端发送。</td>
            <td>否。</td>
            <td>只有用户主动选择目标应用时，目标应用会收到图片/文案。</td>
          </tr>
          <tr>
            <td>通知权限 <code>POST_NOTIFICATIONS</code></td>
            <td>用于本地用药提醒。用户可拒绝或在系统设置中关闭。</td>
            <td>否。</td>
            <td>否。</td>
          </tr>
          <tr>
            <td>开机完成广播 <code>RECEIVE_BOOT_COMPLETED</code></td>
            <td>设备重启后重新安排本地提醒，不读取通讯录、位置、照片或其它私人内容。</td>
            <td>否。</td>
            <td>否。</td>
          </tr>
          <tr>
            <td>系统日历 / 提醒 Intent</td>
            <td>用户选择“添加到系统提醒/日历”时，应用把计划标题、时间和备注交给系统日历创建界面，由用户确认。</td>
            <td>否。</td>
            <td>可能由用户选择的日历应用处理；HRT Recorder 不读取日历，也不后台同步日历。</td>
          </tr>
          <tr>
            <td>X / Twitter 应用检测</td>
            <td>应用会在分享时临时检查设备上是否存在 X / Twitter 客户端，以便直接打开；结果不保存、不上传。</td>
            <td>否。</td>
            <td>否。</td>
          </tr>
          <tr>
            <td>语言、主题、首次同意状态等设置</td>
            <td>用于本机个性化显示和避免重复弹出首次协议。</td>
            <td>否。</td>
            <td>否。</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section>
      <h2>三、数据存储、保留与删除</h2>
      <ul>
        <li>应用数据默认保存在 Android 应用私有存储中，开发者无法远程访问。</li>
        <li>卸载应用、清除应用数据，或在应用内删除记录，会删除本机对应数据。</li>
        <li>用户主动导出的 JSON、CSV、HTML、PNG、截图或发送给其它应用的数据，由用户自行管理和删除。</li>
        <li>应用不创建账号，因此不存在服务器账号删除流程；如 Google Play 需要账号删除 URL，本应用应声明“无账号系统”。</li>
      </ul>
    </section>

    <section>
      <h2>四、安全与第三方服务</h2>
      <p>应用本体没有广告 SDK、统计 SDK、登录 SDK、云同步 SDK，也不包含用于跟踪用户的远程配置。应用不使用 Android Advertising ID，不读取精确位置、通讯录、短信、通话记录、麦克风、相机、照片库或持久设备标识符。</p>
      <p>当用户访问本隐私政策网页时，网页托管服务 Cloudflare 可能为了安全、防滥用、缓存和网络传输处理常规 Web 请求信息，例如 IP 地址、User-Agent、请求时间和访问路径。该处理发生在访问网页时，不代表 HRT Recorder 应用会上传应用内数据。本页面不使用广告、第三方统计脚本或追踪 Cookie。</p>
    </section>

    <section id="medical" class="warning">
      <h2>五、健康与医疗免责声明</h2>
      <p><strong>HRT Recorder 不是医疗器械，不提供医疗服务，不用于诊断、治疗、治愈或预防任何疾病或医疗状况。</strong></p>
      <p>应用中的 E2、CPA、Testosterone、抗雄相关曲线、参考区间、校准、置信区间、异常提示、罩杯计算和任何分享图片都仅用于个人记录、趋势参考与数据整理。它们不能替代医生、药师、实验室报告或专业医疗建议。任何用药、停药、剂量、路线、检查和健康决策，请咨询合格医疗专业人士。</p>
    </section>

    <section id="terms">
      <h2>六、用户协议</h2>
      <h3>1. 使用范围</h3>
      <p>用户可以将 HRT Recorder 用于个人离线记录、趋势估算、化验录入、计划提醒、药瓶库存管理、JSON/CSV 导入导出和分享图片生成。用户不得利用本软件侵犯他人隐私、传播恶意文件、冒充他人、从事违法用途或误导他人将估算值视为医疗结论。</p>
      <h3>2. 用户责任</h3>
      <p>用户应自行确认输入数据的准确性，并理解不同模型、单位、给药路线、个体差异、化验误差和参考资料差异都可能导致估算偏差。用户对自己导入、导出、截图、分享和发送的数据负责。</p>
      <h3>3. 知识产权与参考来源</h3>
      <p>HRT Recorder Android 版由南盺实现。项目可能在代码、文件或说明层参考 Journey、HRT-Recorder-online、hrt.mahiro.uk、Transmtf-HRT-Tracker、MtF-wiki 等公开资料或项目。参考来源不构成用户界面作者署名，也不表示这些来源对本应用负责。</p>
      <h3>4. 免责声明</h3>
      <p>在法律允许范围内，本软件按“现状”提供，不保证估算准确性、适用性、连续可用性或完全无错误。开发者不对用户基于软件内容作出的医疗、用药、财务、社交或其它决定承担责任。</p>
      <h3>5. 协议更新</h3>
      <p>当功能、权限、数据处理方式或法律要求发生变化时，本页面可能更新。更新后的日期会显示在页面顶部。继续使用更新版本，表示用户理解并接受更新后的说明。</p>
    </section>

    <section id="english">
      <h2>English Summary</h2>
      <p><strong>HRT Recorder</strong> is an offline Android record and trend-estimation tool by Nanxin. Package name: <code>com.nanxin.hrtrecorder</code>. Contact: <a href="mailto:wangyanluo233@gmail.com">wangyanluo233@gmail.com</a>.</p>
      <p>The app does not request Internet permission and does not include login, ads, analytics, cloud sync, or remote configuration. Medication records, lab results, weight, measurements, plans, reminders, bottle inventory, settings, imported files and generated share images are stored locally on the user’s device. The developer does not collect, upload, sell, or share app data.</p>
      <p>Android permissions and device capabilities are used only for user-facing local functions: notifications for local reminders, boot completed to reschedule reminders after reboot, Android file picker for import/export, Android share sheet/FileProvider for user-initiated image sharing, and a calendar insert Intent when the user chooses to create a system calendar reminder.</p>
      <p>HRT Recorder is not a medical device and does not provide medical advice, diagnosis, prescriptions, dosage recommendations, treatment, cure, or prevention of any medical condition. All curves and estimates are for personal reference only.</p>
    </section>
  </main>

  <footer>
    <p>© 2026 南盺 · HRT Recorder · <a href="https://x.com/xynMTFxyn">作者主页</a> · <a href="mailto:wangyanluo233@gmail.com">联系邮箱</a></p>
  </footer>
</body>
</html>`;

export default {
  async fetch(request) {
    const url = new URL(request.url);
    if (url.pathname === "/health") {
      return new Response("ok", {
        headers: { "content-type": "text/plain; charset=utf-8" },
      });
    }
    return new Response(html, {
      headers: {
        "content-type": "text/html; charset=utf-8",
        "cache-control": "public, max-age=300",
        "x-robots-tag": "index, follow",
      },
    });
  },
};
