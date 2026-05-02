import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import { Download } from 'lucide-react';

export default function LessonPDFExporter({ targetRef, fileName = 'lesson.pdf' }) {
  async function download() {
    if (!targetRef.current) return;
    const canvas = await html2canvas(targetRef.current, {
      scale: 2,
      backgroundColor: '#fff8ed',
    });
    const image = canvas.toDataURL('image/png');
    const pdf = new jsPDF('p', 'mm', 'a4');
    const width = pdf.internal.pageSize.getWidth();
    const height = (canvas.height * width) / canvas.width;
    pdf.addImage(image, 'PNG', 0, 0, width, height);
    pdf.save(fileName);
  }

  return (
    <button className="ghost-button" onClick={download}>
      <Download size={16} /> Download PDF
    </button>
  );
}

