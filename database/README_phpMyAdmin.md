# ربط التطبيق بـ phpMyAdmin (MySQL / MariaDB)

## 1. استيراد قاعدة البيانات في phpMyAdmin

1. افتح **phpMyAdmin** (مثلاً من XAMPP: `http://localhost/phpmyadmin`).
2. من الأعلى اختر تبويب **Import** (استيراد).
3. اضغط **Choose File** واختر الملف:
   ```
   AppointmentSystem/database/full_schema_mysql_phpmyadmin.sql
   ```
4. اضغط **Go** (تنفيذ) في الأسفل.
5. بعد النجاح ستجد قاعدة بيانات باسم **appointment** وجميع الجداول فيها.

## 2. الجداول المُنشأة

| الجدول | الوصف |
|--------|--------|
| **app_user** | المستخدمون (مرضى، أطباء، إداريون، استقبال) |
| **clinic** | العيادات / الفروع |
| **doctor** | الأطباء |
| **room** | الغرف |
| **appointment** | المواعيد |
| **pending_task** | المهام المعلقة |
| **waitlist_entry** | قائمة الانتظار |
| **audit_entry** | سجل التدقيق |
| **system_settings** | إعدادات النظام |

## 3. إعداد التطبيق للاتصال بـ MySQL

في الملف **`src/main/resources/application.properties`**:

1. عطّل إعدادات PostgreSQL (ضع `#` أمام الأسطر أو احذفها).
2. فعّل إعدادات MySQL بهذا الشكل:

```properties
database.enabled=true
database.url=jdbc:mysql://localhost:3306/appointment?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
database.username=root
database.password=
```

- **username**: عادةً `root` في XAMPP، أو اسم المستخدم الذي أنشأته في MySQL.
- **password**: كلمة مرور MySQL (في XAMPP الافتراضي غالباً فارغ).
- إذا كان MySQL على بورت غير 3306 غيّر الرقم في الرابط.

## 4. تشغيل التطبيق

بعد حفظ الملف أعد تشغيل التطبيق. إذا ظهرت رسالة "Database not connected" تحقق من:

- تشغيل MySQL (من XAMPP: Start عند Apache و MySQL).
- اسم المستخدم وكلمة المرور في `application.properties`.
- أنك استوردت ملف `full_schema_mysql_phpmyadmin.sql` بنجاح (يظهر في phpMyAdmin قاعدة **appointment** والجداول).

## 5. عرض البيانات في phpMyAdmin

- **جدول المواعيد الكامل:** من القائمة اليسرى اختر **appointment** ثم الجدول **appointment** (كل الأعمدة).
- **عرض مبسّط للمواعيد:** اختر **v_appointments_simple** (عرض) لعرض المواعيد بأعمدة أساسية فقط: id, patient_id, doctor_id, clinic_id, start_time, end_time, status, appointment_type, created_at.

## 6. إذا لم تظهر المواعيد بعد الحجز

1. تأكد أن التطبيق متصل بقاعدة البيانات (لم تظهر نافذة "Database not connected" عند البدء).
2. عند فشل الحجز ستظهر رسالة الخطأ الفعلية — اقرأها (مثلاً: تعارض وقت، أو خطأ في قاعدة البيانات).
3. تأكد أنك سجّلت الدخول بحساب موجود في قاعدة البيانات (تسجيل دخول بعد استيراد الـ schema يضمن ذلك).
4. في phpMyAdmin تحقق من جدول **app_user** أن هناك مستخدمين، ثم من **appointment** أو **v_appointments_simple** أن المواعيد تُسجّل.
