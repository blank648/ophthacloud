import html2pdf from 'html2pdf.js';

/**
 * Print a specific HTML element or selector reliably across browsers and Tauri WebView.
 * Uses html2pdf stream rendering with window/iframe fallback for maximum compatibility.
 */
export async function printElement(elementOrSelector: HTMLElement | string, documentTitle: string = 'Document'): Promise<void> {
  let targetElement: HTMLElement | null = null;

  if (typeof elementOrSelector === 'string') {
    targetElement = document.querySelector(elementOrSelector);
  } else {
    targetElement = elementOrSelector;
  }

  if (!targetElement) {
    console.error(`[printElement] Target element not found:`, elementOrSelector);
    return;
  }

  // Clone element & strip non-printable components
  const cloned = targetElement.cloneNode(true) as HTMLElement;
  cloned.querySelectorAll('.no-print').forEach((el) => el.remove());

  const cleanFilename = `${documentTitle.replace(/[^a-zA-Z0-9_\-\s]/g, '_')}.pdf`;

  const opt = {
    margin: [10, 10, 10, 10] as [number, number, number, number],
    filename: cleanFilename,
    image: { type: 'jpeg' as const, quality: 0.98 },
    html2canvas: {
      scale: 2,
      useCORS: true,
      logging: false,
      windowWidth: 800,
    },
    jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' as const },
  };

  try {
    // 1. Generate PDF blob via html2pdf
    const pdfWorker = html2pdf().set(opt).from(cloned);
    const pdfBlob = await pdfWorker.output('blob');
    const blobUrl = URL.createObjectURL(pdfBlob);

    // 2. Open printable blob URL in window
    const printWindow = window.open(blobUrl, '_blank');
    if (printWindow) {
      printWindow.focus();
      setTimeout(() => {
        try {
          printWindow.print();
        } catch (_) {}
      }, 500);
    } else {
      // 3. If window.open is blocked in Tauri webview or popup blocker: save/download directly
      await pdfWorker.save();
    }
  } catch (err) {
    console.warn('[printElement] html2pdf print stream failed, attempting iframe print:', err);

    // Fallback: Offscreen rendered iframe (visible to layout engine but positioned offscreen)
    const iframe = document.createElement('iframe');
    iframe.style.position = 'fixed';
    iframe.style.left = '-9999px';
    iframe.style.top = '0';
    iframe.style.width = '800px';
    iframe.style.height = '1000px';
    iframe.style.border = 'none';
    iframe.style.opacity = '0.01';
    iframe.style.pointerEvents = 'none';
    iframe.style.zIndex = '-9999';

    document.body.appendChild(iframe);

    const doc = iframe.contentDocument || iframe.contentWindow?.document;
    if (!doc) {
      console.error('[printElement] Could not access iframe document');
      if (document.body.contains(iframe)) document.body.removeChild(iframe);
      return;
    }

    const stylesHTML = Array.from(document.querySelectorAll('style, link[rel="stylesheet"]'))
      .map((node) => node.outerHTML)
      .join('\n');

    doc.open();
    doc.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>${documentTitle}</title>
          <meta charset="utf-8" />
          ${stylesHTML}
          <style>
            body {
              background: white !important;
              color: black !important;
              margin: 0 !important;
              padding: 20px !important;
              font-family: system-ui, -apple-system, sans-serif !important;
            }
            .no-print {
              display: none !important;
            }
            @page {
              margin: 10mm;
              size: auto;
            }
          </style>
        </head>
        <body>
          <div class="print-container">
            ${targetElement.outerHTML}
          </div>
        </body>
      </html>
    `);
    doc.close();

    setTimeout(() => {
      try {
        iframe.contentWindow?.focus();
        iframe.contentWindow?.print();
      } catch (e) {
        console.error('[printElement] Fallback iframe print failed:', e);
      } finally {
        setTimeout(() => {
          if (document.body.contains(iframe)) {
            document.body.removeChild(iframe);
          }
        }, 1000);
      }
    }, 300);
  }
}

/**
 * Export a specific HTML element or selector directly as a PDF download file.
 */
export async function downloadPDF(elementOrSelector: HTMLElement | string, filename: string = 'document.pdf'): Promise<void> {
  let targetElement: HTMLElement | null = null;

  if (typeof elementOrSelector === 'string') {
    targetElement = document.querySelector(elementOrSelector);
  } else {
    targetElement = elementOrSelector;
  }

  if (!targetElement) {
    console.error(`[downloadPDF] Target element not found:`, elementOrSelector);
    return;
  }

  const cleanFilename = filename.endsWith('.pdf') ? filename : `${filename}.pdf`;

  // Clone element to manipulate print-only styling if needed
  const cloned = targetElement.cloneNode(true) as HTMLElement;
  cloned.querySelectorAll('.no-print').forEach((el) => el.remove());

  const opt = {
    margin: [10, 10, 10, 10] as [number, number, number, number],
    filename: cleanFilename,
    image: { type: 'jpeg' as const, quality: 0.98 },
    html2canvas: {
      scale: 2,
      useCORS: true,
      logging: false,
      windowWidth: 800,
    },
    jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' as const },
  };

  try {
    await html2pdf().set(opt).from(cloned).save();
  } catch (err) {
    console.error('[downloadPDF] Error generating PDF:', err);
    // Fallback to print if html2pdf fails
    printElement(targetElement, cleanFilename);
  }
}
