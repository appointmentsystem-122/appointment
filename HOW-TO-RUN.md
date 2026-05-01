# تشغيل التطبيق / How to Run

## من Maven (موصى به)
```bash
cd AppointmentSystem
mvn javafx:run
```

## من الـ IDE (IntelliJ / Eclipse / VS Code)
1. افتح المشروع كمشروع Maven.
2. حدد الـ Main class: `com.appointmentscheduler.presentation.MainApp`
3. شغّل (Run).

إذا ظهر خطأ **"JavaFX runtime components are missing"**:
- أضف VM options عند التشغيل:
  ```
  --module-path "المسار_إلى_javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics
  ```
- أو استخدم تشغيل Maven: **Run → Maven → javafx:run**

## إذا لم تظهر نافذة أو التطبيق يتوقف
- التطبيق يعرض الآن **نافذة خطأ** مع رسالة توضيحية عند أي فشل (تحميل الشاشة، تهيئة الخدمات، إلخ).
- راجع **Console / Run** في الـ IDE لأي stack trace أو رسائل لوق.
- تأكد أن مجلد `src/main/resources` مضاف إلى classpath وأن ملفات FXML و CSS موجودة تحت:
  `com/appointmentscheduler/presentation/`

## بيانات الدخول التجريبية (تُنشأ تلقائياً عند أول تشغيل بدون مستخدمين)
- Admin: `admin@admin.com` / `admin123`
- عميل: `customer@example.com` / `password123`
- مقدم خدمة: `provider@example.com` / `doctor123`
- استقبال: `staff@example.com` / `reception123`

إذا استوردت قاعدة من SQL، فالإيميلات قد تختلف — راجع جدول المستخدمين في قاعدتك.
