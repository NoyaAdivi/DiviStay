<div dir="rtl">

<p align="center">
  <a href="https://divistay.onrender.com"><img src="docs/readme/banner-he.svg" width="100%" alt="DiviStay: הזמנת מלון קבוצתית עם פיצול תשלום"></a>
</p>

<p align="center"><a href="README.md"><img src="docs/readme/lang-en.svg" height="44" alt="Read in English"></a></p>

<p align="center">
  <a href="https://divistay.onrender.com"><img src="https://img.shields.io/badge/%D7%9C%D7%90%D7%AA%D7%A8_%D7%94%D7%97%D7%99-FFD5C9?style=for-the-badge&logo=render&logoColor=A8483A" alt="לאתר החי"></a>
  <a href="#s1"><img src="https://img.shields.io/badge/%D7%90%D7%99%D7%9A_%D7%96%D7%94_%D7%A2%D7%95%D7%91%D7%93-DAD5FF?style=for-the-badge&logo=spring&logoColor=5548C8" alt="איך זה עובד"></a>
  <a href="docs/"><img src="https://img.shields.io/badge/%D7%9E%D7%A1%D7%9E%D7%9B%D7%99_%D7%94%D7%AA%D7%9B%D7%A0%D7%95%D7%9F-D9F3E7?style=for-the-badge&logo=readthedocs&logoColor=1F7350" alt="מסמכי התכנון"></a>
</p>

<p align="center">
  <!--<img src="https://img.shields.io/github/license/NoyaAdivi/DiviStay?color=DAD5FF&labelColor=5548C8" alt="License">-->
  <a href="https://github.com/NoyaAdivi/DiviStay/commits/main"><img src="https://img.shields.io/github/last-commit/NoyaAdivi/DiviStay?color=FFD5C9&labelColor=A8483A" alt="Last commit"></a>
  <a href="https://github.com/NoyaAdivi/DiviStay"><img src="https://img.shields.io/github/languages/top/NoyaAdivi/DiviStay?color=D9F3E7&labelColor=1F7350" alt="Top language"></a>
  <a href="https://github.com/NoyaAdivi/DiviStay"><img src="https://img.shields.io/github/repo-size/NoyaAdivi/DiviStay?color=FBD5E0&labelColor=A03A5C" alt="Repo size"></a>
</p>

<h3 align="center">המארגן בוחר חדרים, וכל אחד משלם מקישור אישי.</h3>

<p align="center">
  ההזמנה מאושרת רק כשכל הקבוצה שילמה, ואם היא לא יוצאת לפועל אף אחד לא מחויב.
</p>

<p align="center">
  <a href="https://divistay.onrender.com"><img src="docs/logo.png" width="150" alt="הלוגו של DiviStay"></a>
</p>

<p align="center">
  <a href="https://divistay.onrender.com"><img src="docs/screenshot-he.png" width="100%" alt="דף הבית של DiviStay"></a>
</p>

<p align="center"><sub>פרויקט הדגמה: התשלומים, החדרים, המחירים והביקורות מדומים. הטעינה הראשונה יכולה לקחת כ־30 שניות.</sub></p>

> 📐 התחיל כפרויקט **ארכיטקטורה ודרישות תוכנה** ([DiviStay Architecture](https://github.com/NoyaAdivi/DiviStay_Architecture), SRS ו־UML בתיקייה [`docs/`](docs/)), תוכנן בצוות של 3, ואז **מומש ב־Java 21 וב־Spring Boot** ועלה לאוויר.

<table align="center">
  <tr>
    <td align="center" width="25%"><a href="#s3"><b>🏨 18,000+</b></a><br><sub>מלונות אמיתיים</sub></td>
    <td align="center" width="25%"><a href="#s1"><b>⏱️ 30 דק'</b></a><br><sub>החזקת חדרים אטומית</sub></td>
    <td align="center" width="25%"><a href="#s3"><b>🧪 45</b></a><br><sub>בדיקות JUnit</sub></td>
    <td align="center" width="25%"><a href="https://divistay.onrender.com"><b>🌍 HE · EN</b></a><br><sub>עברית ואנגלית</sub></td>
  </tr>
</table>

<a name="s1"></a>
<p align="center"><a href="#s1"><img src="docs/readme/s1-he.svg" width="100%" alt="איך זה עובד"></a></p>


1. **חיפוש ונעילה:** המארגן בוחר חדרים, והם ננעלים ל-**30 דקות** כדי שאף אחד אחר לא יזמין אותם (מניעת הזמנת יתר).
2. **פיצול:** העלות מתחלקת **שווה בשווה**, **לפי חדר** (מי שבסוויטה משלם יותר) או **בסכומים מותאמים אישית**.
3. **תשלום:** כל חבר מקבל קישור תשלום אישי שאי אפשר לנחש. התשלום *מקפיא* את הסכום בכרטיס.
4. **אישור:** כשמגיעים ל-100%, ההזמנה נשלחת למלון. אם המלון אישר, הסכומים *נגבים* ונשלחות קבלות. אם מערכת המלון לא עונה, המערכת **מנסה שוב** עם המתנה שהולכת וגדלה.
5. **ביטול אוטומטי:** אם הזמן נגמר, או שהמלון לא אישר, החדרים משתחררים וכל הקפאה **מבוטלת**, כך שאף אחד לא מחויב.

<a name="s2"></a>
<p align="center"><a href="#s2"><img src="docs/readme/s2-he.svg" width="100%" alt="תבניות עיצוב"></a></p>


| תבנית | איפה | למה |
|---|---|---|
| **Strategy** | `SplitStrategy`, ושלוש המימושים שלו | לוגיקת ההזמנה לא יודעת איך העלות מתחלקת. כלל פיצול חדש הוא מחלקה אחת חדשה. |
| **Factory** | `SplitStrategyFactory`, `RoomFactory` | מקום אחד מחליט באיזו אסטרטגיה להשתמש ואיך נבנה כל סוג חדר. |
| **Adapter** | `PaymentGateway`, `MockPayGatewayAdapter`, `MockPayClient` | הליבה מדברת עם ממשק תשלום משלה. החלפת ספק תשלום היא Adapter חדש. |
| **Ports & adapters** | `HotelPmsClient`, `NotificationService`, ה-repositories | בליבה **אין תלות ב-Spring**. Spring רק מחבר את החלקים (`AppConfig`). |

<a name="s3"></a>
<p align="center"><a href="#s3"><img src="docs/readme/s3-he.svg" width="100%" alt="פרטים הנדסיים"></a></p>


- **מלונות אמיתיים:** יותר מ-18,000 מלונות אמיתיים ב-60 יעדים ו-21 מדינות, מתוך OpenStreetMap (רישיון ODbL): שם, כתובת, מיקום וכוכבים כשידועים. החדרים, המחירים וחוות הדעת מדומים (אין גישה לזמינות אמיתית), והתמונות מתאימות לסוג המקום: עיר, חוף, אי יווני או ריזורט.
- **חיפוש לפי עיר או מדינה,** בעברית או באנגלית, ועמוד יעדים לפי מדינות.
- **חיפוש כמו באתר אמיתי:** מבוגרים, ילדים עם גילאים, וכמה מתחלקים בתשלום. לכל מלון מחושב צירוף החדרים הזול ביותר שמספיק לכל הקבוצה (בעיית תרמיל קטנה), עם המחיר לכל משלם. סינון לפי כוכבים, דירוג, מחיר ומתקנים, מיון ועמודים.
- **מקביליות:** נעילת חדרים היא פעולה אטומית של "הכול או כלום". בדיקה מריצה 32 תהליכונים שמתחרים על אותו חדר ומוודאת שרק אחד מצליח.
- **כסף:** נשמר באגורות שלמות (`long`), אף פעם לא כמספר עשרוני, ולכן הפיצול תמיד מסתכם בדיוק.
- **אבטחה:** קישורי התשלום ולוח הבקרה הם מחרוזות אקראיות של 256 ביט, ונשמר רק ה-hash שלהן. פרטי כרטיס לא נשמרים לעולם.
- **Idempotency:** אותו חבר שמשלם עם אותו כרטיס פעמיים (לחיצה כפולה) לא יחויב פעמיים.
- **מכונת מצבים:** `BookingGroup` מאפשר רק מעברים חוקיים בין המצבים.
- **שתי שפות:** אנגלית ועברית (מימין לשמאל) עם `MessageSource` של Spring. שפת הדפדפן מזוהה אוטומטית והבחירה נשמרת. הודעות שמגיעות מהליבה מתורגמות בשכבת ה-web (`Translator`), כך שהלוגיקה העסקית לא תלויה בשפה.
- **נגישות:** תפריט נגישות (גודל טקסט, ניגודיות גבוהה, הדגשת קישורים, גופן קריא, עצירת אנימציות וסמן גדול), ניווט מלא במקלדת, ניגודיות צבעים ברמת AA והצהרת נגישות.
- **מצב כהה:** לפי הגדרת המערכת, עם אפשרות להחליף ידנית.
- **עמודים משפטיים:** מדיניות פרטיות, תנאי שימוש והצהרת נגישות, בשתי השפות.
- **בדיקות:** JUnit 5 לכללי הפיצול, לנעילה ולמקביליות, לכל מחזור החיים, ל-Adapter של התשלום, ובדיקת קצה-לקצה של האתר עם MockMvc.

<a name="s4"></a>
<p align="center"><a href="#s4"><img src="docs/readme/s4-he.svg" width="100%" alt="הרצה"></a></p>


**בווינדוס, בלי שום הגדרה:** לחיצה כפולה על `START-DIVISTAY.bat`. הקובץ מוצא Java 21 ומעלה במחשב, מוריד את Maven בפעם הראשונה, מפעיל את האתר ופותח את הדפדפן. `TEST-DIVISTAY.bat` מריץ את כל הבדיקות.

צריך **Java 21 ומעלה**. אפשר גם עם **IntelliJ IDEA**:

1. `File → Open`, ובוחרים את התיקייה הזו (זו שיש בה `pom.xml`). מחכים ש-Maven יוריד את הספריות.
2. פותחים את `src/main/java/com/divistay/DiviStayApplication.java` ולוחצים ▶ **Run**.
3. נכנסים ל-**http://localhost:8080**.

**לנסות:** יוצרים קבוצה עם 2-3 כתובות מייל, פותחים את קישורי התשלום מתיבת *Messages sent* בלוח הבקרה, ומשלמים עם כרטיס הבדיקה `4242 4242 4242 4242` (כל תאריך עתידי וכל CVC).

<a name="s5"></a>
<p align="center"><a href="#s5"><img src="docs/readme/s5-he.svg" width="100%" alt="העלאה לאינטרנט"></a></p>


האתר הוא שרת Java, ולכן הוא לא עולה ל-Vercel. הדרך הפשוטה היא [Render](https://render.com), שיש לו מסלול חינמי:

1. מעלים את הפרויקט ל-GitHub (מאגר חדש, למשל `DiviStay`).
2. נכנסים ל-Render עם חשבון GitHub, ובוחרים **New → Blueprint** ואת המאגר. הקובץ `render.yaml` כבר מגדיר הכל, ו-Render בונה את האתר לפי ה-`Dockerfile`.
3. אחרי כמה דקות מקבלים כתובת כמו `https://divistay.onrender.com`.

כדאי לדעת: במסלול החינמי השרת נרדם אחרי 15 דקות בלי ביקורים, והטעינה הראשונה אחרי זה לוקחת כחצי דקה. כל ההזמנות נשמרות בזיכרון, אז הן מתאפסות כשהשרת מתחיל מחדש. זה בסדר לאתר הדגמה.

<a name="s6"></a>
<p align="center"><a href="#s6"><img src="docs/readme/s6-he.svg" width="100%" alt="היקף וצעדים הבאים"></a></p>


התשלומים ומערכת המלון **מדומים** (אין כסף אמיתי), ההודעות מוצגות על המסך במקום במייל, והמידע נשמר בזיכרון. כל אחד מהם נמצא מאחורי ממשק, ולכן הצעדים הבאים הם Adapters חדשים, לא כתיבה מחדש: ספק תשלום אמיתי, API של מלון, שירות מייל ומסד נתונים.


<p align="center"><sub><i>תוכנן על ידי נויה, עמית ואמיר במסגרת קורס הנדסת תוכנה; מומש על ידי נויה.</i></sub></p>

<a name="contact"></a>
<p align="center"><a href="#contact"><img src="docs/readme/footer-he.svg" width="100%" alt="נבנה על ידי נויה אדיבי"></a></p>

<p align="center">
  <a href="https://divistay.onrender.com"><img src="https://img.shields.io/badge/%D7%9C%D6%BEDiviStay-FFD5C9?style=for-the-badge&logo=github&logoColor=A8483A" alt="ל־DiviStay"></a>
  <a href="https://www.linkedin.com/in/noya-adivi/"><img src="https://img.shields.io/badge/LinkedIn-DAD5FF?style=for-the-badge&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZmlsbD0iIzU1NDhDOCIgZD0iTTIwLjQ1IDIwLjQ1aC0zLjU2di01LjU3YzAtMS4zMy0uMDMtMy4wNC0xLjg1LTMuMDQtMS44NiAwLTIuMTQgMS40NS0yLjE0IDIuOTR2NS42N0g5LjM1VjloMy40MXYxLjU2aC4wNWMuNDgtLjkgMS42NC0xLjg1IDMuMzctMS44NSAzLjYgMCA0LjI3IDIuMzcgNC4yNyA1LjQ2djYuMjh6TTUuMzQgNy40M2EyLjA2IDIuMDYgMCAxIDEgMC00LjEzIDIuMDYgMi4wNiAwIDAgMSAwIDQuMTN6TTcuMTIgMjAuNDVIMy41NlY5aDMuNTZ2MTEuNDV6TTIyLjIyIDBIMS43N0MuNzkgMCAwIC43NyAwIDEuNzN2MjAuNTRDMCAyMy4yMy43OSAyNCAxLjc3IDI0aDIwLjQ1Yy45OCAwIDEuNzgtLjc3IDEuNzgtMS43M1YxLjczQzI0IC43NyAyMy4yIDAgMjIuMjIgMHoiLz48L3N2Zz4=" alt="LinkedIn"></a>
  <a href="https://noyaadivi.github.io/noya-website"><img src="https://img.shields.io/badge/%D7%AA%D7%99%D7%A7_%D7%A2%D7%91%D7%95%D7%93%D7%95%D7%AA-D9F3E7?style=for-the-badge&logo=githubpages&logoColor=1F7350" alt="תיק עבודות"></a>
  <a href="https://github.com/NoyaAdivi"><img src="https://img.shields.io/badge/%D7%A2%D7%95%D7%93_%D7%A4%D7%A8%D7%95%D7%99%D7%A7%D7%98%D7%99%D7%9D-FBD5E0?style=for-the-badge&logo=github&logoColor=A03A5C" alt="עוד פרויקטים"></a>
</p>

</div>
