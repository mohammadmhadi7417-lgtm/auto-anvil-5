# رفع خطای Build در AutoAnvil 1.21.11

خطای نسخه قبلی از این بود که Gradle نمی‌توانست Plugin Marker مربوط به `net.fabricmc.fabric-loom-remap:1.13.4` را پیدا کند.

در این نسخه Loom به `1.14.10` تغییر داده شده است؛ این نسخه Plugin Marker رسمی `net.fabricmc.fabric-loom-remap.gradle.plugin` را در Maven فابریک دارد.

## GitHub Actions

1. محتوای این ZIP را در ریشه Repository قرار بده.
2. در GitHub وارد **Actions** شو.
3. Workflow با نام **Build AutoAnvil** را باز کن.
4. روی **Run workflow** بزن یا یک commit/push انجام بده.
5. بعد از موفقیت، پایین صفحه بخش **Artifacts** ظاهر می‌شود.
6. فایل `AutoAnvil-1.21.11` را دانلود کن.

اگر Build بعد از این مرحله خطای دیگری داد، همان خطای جدید را بفرست؛ آن خطا دیگر مربوط به پیدا نشدن Loom نیست و می‌توانیم مرحله بعدی را اصلاح کنیم.
