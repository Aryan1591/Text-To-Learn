import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import { Download } from 'lucide-react';

export default function LessonPDFExporter({ targetRef, fileName = 'lesson.pdf' }) {
  async function download() {
    if (!targetRef.current) return;
    const canvas = await html2canvas(targetRef.current, {
      scale: 2,
      backgroundColor: '#fff8ed',
      useCORS: true,
      scrollY: -window.scrollY
    });
    const pdf = new jsPDF('p', 'mm', 'a4');
    const pageWidthMm = pdf.internal.pageSize.getWidth();
    const pageHeightMm = pdf.internal.pageSize.getHeight();
    const pageWidthPx = canvas.width;
    const pageHeightPx = Math.floor((canvas.width * pageHeightMm) / pageWidthMm);

    let renderedHeight = 0;
    let firstPage = true;
    while (renderedHeight < canvas.height) {
      const sliceHeight = Math.min(pageHeightPx, canvas.height - renderedHeight);
      const pageCanvas = document.createElement('canvas');
      pageCanvas.width = pageWidthPx;
      pageCanvas.height = sliceHeight;
      const ctx = pageCanvas.getContext('2d');
      if (!ctx) return;

      ctx.drawImage(
        canvas,
        0,
        renderedHeight,
        pageWidthPx,
        sliceHeight,
        0,
        0,
        pageWidthPx,
        sliceHeight
      );

      const pageImgData = pageCanvas.toDataURL('image/png');
      const pageRenderedHeightMm = (sliceHeight * pageWidthMm) / pageWidthPx;

      if (!firstPage) {
        pdf.addPage();
      }
      pdf.addImage(pageImgData, 'PNG', 0, 0, pageWidthMm, pageRenderedHeightMm, undefined, 'FAST');

      renderedHeight += sliceHeight;
      firstPage = false;
    }

    pdf.save(fileName);
  }

  return (
    <button className="ghost-button" onClick={download}>
      <Download size={16} /> Download PDF
    </button>
  );
}
