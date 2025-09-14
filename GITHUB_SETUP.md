# إعداد المشروع على GitHub

هذا الملف يشرح كيفية إعداد المشروع على GitHub واستخدام GitHub Actions لبناء التطبيق تلقائياً.

## الخطوات

### 1. إنشاء مستودع (Repository) جديد على GitHub

1. قم بتسجيل الدخول إلى حسابك على [GitHub](https://github.com/)
2. انقر على زر "+" في أعلى الصفحة واختر "New repository"
3. أدخل اسم المستودع: `AzkarSabahSimple`
4. اختر ما إذا كنت تريد أن يكون المستودع عاماً أو خاصاً
5. لا تقم بإضافة ملف README أو .gitignore أو الترخيص (سنقوم برفع الملفات الموجودة)
6. انقر على "Create repository"

### 2. رفع المشروع إلى GitHub

افتح Terminal (موجه الأوامر) وقم بتنفيذ الأوامر التالية:

```bash
# انتقل إلى مجلد المشروع
cd AzkarSabahSimple

# قم بتهيئة Git
git init

# أضف جميع الملفات
git add .

# قم بعمل commit
git commit -m "النسخة الأولية من تطبيق أذكار الصباح"

# قم بإضافة الرابط الخاص بمستودعك (استبدل USERNAME بإسم المستخدم الخاص بك)
git remote add origin https://github.com/USERNAME/AzkarSabahSimple.git

# ادفع الكود إلى GitHub
git push -u origin master
```

### 3. التحقق من GitHub Actions

1. انتقل إلى مستودعك على GitHub
2. انقر على تبويب "Actions"
3. سترى أن عملية البناء قد بدأت تلقائياً بعد دفع الكود
4. انتظر حتى تكتمل العملية
5. بعد اكتمال العملية، انقر عليها ثم انتقل إلى قسم "Artifacts"
6. يمكنك تنزيل ملف APK من هناك

### 4. تشغيل البناء يدوياً

1. انتقل إلى تبويب "Actions" في مستودعك
2. اختر "Build Android App" من القائمة على اليسار
3. انقر على زر "Run workflow"
4. اختر الفرع الذي تريد البناء منه (عادة "master" أو "main")
5. انقر على زر "Run workflow" الأخضر

### 5. تعديل شارة حالة البناء في README

قم بتعديل الرابط في ملف README.md ليعكس اسم المستخدم الخاص بك:

```markdown
![Build Status](https://github.com/USERNAME/AzkarSabahSimple/workflows/Build%20Android%20App/badge.svg)
```

استبدل `USERNAME` باسم المستخدم الخاص بك على GitHub.

## استكشاف الأخطاء وإصلاحها

إذا واجهت أي مشاكل في عملية البناء، يمكنك التحقق من سجل الأخطاء في تبويب Actions على GitHub. بعض المشاكل الشائعة وحلولها:

1. **خطأ في صلاحيات gradlew**: تأكد من أن ملف gradlew لديه صلاحيات التنفيذ قبل الدفع إلى GitHub.
   ```bash
   git update-index --chmod=+x gradlew
   git commit -m "جعل gradlew قابل للتنفيذ"
   git push
   ```

2. **مشاكل في إصدار JDK**: إذا كان التطبيق يتطلب إصدار مختلف من JDK، قم بتعديل ملف `.github/workflows/build.yml` وغير قيمة `java-version`.

3. **مشاكل في إصدار Gradle**: تأكد من توافق إصدار Gradle مع إصدار Android Gradle Plugin المستخدم في المشروع.

