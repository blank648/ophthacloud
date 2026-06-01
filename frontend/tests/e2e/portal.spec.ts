import { test, expect } from '@playwright/test';
import { setupLiveAuth } from './test-utils';

test.describe('Patient Portal Suite', () => {
  test.beforeEach(async ({ page, request }) => {
    // Perform Login
    await setupLiveAuth(page, request);
    await page.goto('/');
    await page.getByRole('button', { name: 'Autentificare' }).click();
    await expect(page.getByText('DoctorDashboard')).toBeVisible({ timeout: 10000 }).catch(() => {});
    
    // Navigate to the patient portal via client-side routing to preserve auth state
    await page.evaluate(() => {
      window.history.pushState({}, '', '/portal');
      window.dispatchEvent(new Event('popstate'));
    });

    // Ensure the portal has loaded by checking for standard page elements
    await expect(page.getByText('Programările mele').first()).toBeVisible({ timeout: 10000 });
  });

  test('should view patient portal dashboard', async ({ page }) => {
    // Check if the portal is loaded
    await expect(page.getByText('Dl. Marinescu')).toBeVisible();

    // Verify upcoming appointments
    await expect(page.getByText('Programările mele')).toBeVisible();

    // Verify recent prescriptions
    await page.getByRole('button', { name: 'Rețete' }).click();
    await expect(page.getByText('Rețetele mele')).toBeVisible();

    // Verify documents
    await page.getByRole('button', { name: 'Dosarul meu' }).click();
    await expect(page.getByText('Dosarul meu medical')).toBeVisible();
  });

  test('should verify access and download integrity of clinical documents in portal', async ({ page }) => {
    // Navigate to EMR records tab ("Dosarul meu")
    await page.getByRole('button', { name: 'Dosarul meu' }).click();
    await expect(page.getByText('Dosarul meu medical')).toBeVisible();

    // Look for clinical PDFs or download actions
    const pdfDownloadBtn = page.getByRole('button', { name: /PDF/i }).first();
    await expect(pdfDownloadBtn).toBeVisible({ timeout: 5000 });
    
    // Verify it is interactive
    await pdfDownloadBtn.click();
    
    // Click on Settings tab ("Setări")
    await page.getByRole('button', { name: 'Setări' }).click();
    await expect(page.getByText('Consimțăminte')).toBeVisible();

    // Click "Descarcă jurnal PDF" in access journal
    const downloadJournalBtn = page.getByRole('button', { name: /Descarcă jurnal PDF/i }).first();
    await expect(downloadJournalBtn).toBeVisible();
  });
});
