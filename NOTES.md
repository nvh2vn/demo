# NOTES – ghi chú để viết báo cáo & vấn đáp

> Quy tắc: sau khi đọc/chạy xong mỗi phần, tự viết 3–5 dòng "mình hiểu đoạn này làm gì" bằng lời của bạn.
> Đây là tư liệu cho báo cáo và để trả lời thầy. Đừng dán nguyên văn từ AI.

## Thiết bị đo
- Máy: …
- RAM: …
- Android: …
- Ngày đo: …

## Kỹ thuật 1 – Bitmap downsampling (`ImageLoader.kt`)
Mình hiểu: …
Câu hỏi tự kiểm tra: vì sao ảnh 4000×3000 tốn ~46 MB RAM dù file JPG nhỏ? inSampleSize = 4 giảm bao nhiêu lần bộ nhớ? vì sao lũy thừa của 2?

## Kỹ thuật 2 – LruCache (`ImageLoader.kt`)
Mình hiểu: …
Câu hỏi: LruCache bỏ phần tử nào khi đầy? vì sao 1/8 bộ nhớ? `sizeOf` dùng để làm gì?

## Kỹ thuật 3 – Memory leak (`DetailHelper.kt`)
Mình hiểu: …
Câu hỏi: GC root là gì? vì sao static list giữ Activity gây leak? vì sao lambda trong Handler giữ Activity?

## Kỹ thuật 4 – RecyclerView (`GalleryHost.kt`)
Mình hiểu: …
Câu hỏi: ViewHolder để làm gì? vì sao hủy job trong `onViewRecycled`? vì sao decode trên main thread gây giật (16 ms/frame)?

## Câu hỏi chung
- Từ Android 8 pixel Bitmap nằm ở đâu? Vì sao Java heap không tăng nhiều nhưng app vẫn tốn RAM?
- Bản debug đo khác bản release thế nào? Vì sao?

## Số liệu (điền sau khi đo, xem bảng mẫu trong plan.md mục 6)
…
