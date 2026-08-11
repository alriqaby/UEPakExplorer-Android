# UE PAK Explorer — Android MVP

تطبيق Android أصلي لقراءة واستكشاف Unreal Engine `.pak` بدون Root أو Winlator أو Windows/.NET/Python Runtime.

## أهم شيء للمستخدم غير المبرمج

لا تحتاج Android Studio لبناء النسخة من GitHub.

1. ارفع هذا المجلد إلى مستودع GitHub جديد.
2. افتح تبويب **Actions**.
3. اختر **Build APK**.
4. اضغط **Run workflow**.
5. بعد انتهاء البناء افتح الـworkflow ثم **Artifacts**.
6. حمّل `UE-Pak-Explorer-arm64` وستجد APK جاهزًا لجهاز ARM64.

## التقنية

- Kotlin + Android SDK
- Target SDK 36 / Android 16
- ARM64 (`arm64-v8a`)
- Rust + JNI
- `repak v0.2.3` من GitHub
- Android Storage Access Framework
- لا يتم تحميل PAK كاملًا إلى RAM؛ repak يقرأ الـindex أولًا ويقرأ بيانات الملف عند الاستخراج.

repak يدعم قراءة PAK versions 2–5 و7–9 و11، ويقرأ PAKs المضغوطة والمشفرة. الإصدار 3 تحديدًا يدعم UE4.3–4.15 بحسب جدول التوافق upstream.

## حالة ملفات GTA Vice City Definitive Edition المرفوعة للاختبار

تم فحص الملفين الحقيقيين محليًا.

### arabic.pak

- الحجم: حوالي 29.9 MiB
- PAK Version: **3**
- Mount point: `../../../`
- عدد الملفات: **22**
- الـIndex غير مشفر
- كل الإدخالات التي فُحصت غير مضغوطة
- يحتوي على:
  - 19 ملف `.ufont`
  - ملفي `Game.locres`

المساران الموجودان فعليًا:

```text
Gameface/Content/Localization/GTACommon/Game/en/Game.locres
Gameface/Content/Localization/GTAVC/Game/en/Game.locres
```

### arabic2.pak

- الحجم: حوالي 742 KiB
- PAK Version: **3**
- Mount point: `../../../`
- عدد الملفات: **7**
- الـIndex غير مشفر
- الإدخالات غير مضغوطة
- يحتوي على خطوط Unreal Engine الأساسية `.ufont`

## حدود MVP

هذه النسخة لا تعيد بناء PAK، ولا تحلل `.locres` بعد. الهدف الحالي:

**Open → Parse → Browse → Search → Extract**

دعم Oodle سيُضاف فقط إذا ظهر في ملفات اللعبة كضغط فعلي؛ الملفات المرفوعة حاليًا لا تحتاج Oodle للاستخراج.

## الترخيص

كود التطبيق: MIT.
`repak` مرخص وفق MIT/Apache-2.0؛ راجع مستودع repak قبل توزيع نسخة نهائية.
