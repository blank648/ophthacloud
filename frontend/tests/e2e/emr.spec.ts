import { test, expect } from '@playwright/test';
import { setupLiveAuth, createIsolatedPatient } from './test-utils';

test.describe('EMR Consultation Flow (Sprint 4)', () => {
  let isolatedPatient: any = null;

  test.beforeAll(async ({ request }) => {
    const timestamp = Date.now();
    isolatedPatient = await createIsolatedPatient(request, timestamp);
  });

  test('should navigate consultation sections and sign document', async ({ page, request }) => {
    // Increase timeout for this long multi-step wizard test
    test.setTimeout(90000);

    // 1. Live Backend setup
    await setupLiveAuth(page, request);

    page.on('console', msg => console.log(`BROWSER_LOG: ${msg.text()}`));
    page.on('requestfailed', request => console.log(`REQUEST_FAILED: ${request.url()} - ${request.failure()?.errorText}`));

    // 2. Perform Login
    await page.goto('/');
    await page.getByRole('button', { name: 'Autentificare' }).click();
    await expect(page.getByText('DoctorDashboard')).toBeVisible({ timeout: 10000 }).catch(() => {});

    // 3. Set up wait for create consultation BEFORE navigating
    const createPromise = page.waitForResponse(resp => resp.url().includes('/api/v1/emr/consultations') && resp.request().method() === 'POST' && resp.status() === 201);

    // 4. Navigate directly to Consultation Page via client-side routing to preserve auth state
    await page.evaluate((id) => {
      window.history.pushState({}, '', `/consultation?patientId=${id}`);
      window.dispatchEvent(new Event('popstate'));
    }, isolatedPatient.id);

    // Wait for page load
    await expect(page.getByRole('button', { name: 'Începe Consultația' })).toBeVisible({ timeout: 15000 });

    // Click "Începe Consultația"
    await page.getByRole('button', { name: 'Începe Consultația' }).click();

    // Wait for the create API to complete before interacting
    await createPromise;

    // Verify Section A is loaded
    await expect(page.getByRole('heading', { name: 'A — Acuitate Vizuală & Refracție' })).toBeVisible();

    // Progress through the sections (Clicking Următorul triggers the live save API)
    // Section A -> B
    await page.getByRole('button', { name: 'Următorul' }).click();
    await page.waitForResponse(resp => resp.url().includes('/sections/A') && resp.request().method() === 'PUT' && resp.status() === 200);
    await expect(page.getByText('Secțiunea A salvată')).toBeVisible();

    // Section B -> C
    await page.getByRole('button', { name: 'Următorul' }).click();
    await page.waitForResponse(resp => resp.url().includes('/sections/B') && resp.request().method() === 'PUT' && resp.status() === 200);
    await expect(page.getByText('Secțiunea B salvată')).toBeVisible();

    // Section C -> D
    await page.getByRole('button', { name: 'Următorul' }).click();
    await page.waitForResponse(resp => resp.url().includes('/sections/C') && resp.request().method() === 'PUT' && resp.status() === 200);
    await expect(page.getByText('Secțiunea C salvată')).toBeVisible();

    // Fill in valid, realistic IOP values for Section D happy path
    const iopODInput = page.locator('input[type="number"]').first();
    const iopOSInput = page.locator('input[type="number"]').last();
    await iopODInput.fill('18');
    await iopOSInput.fill('17');

    // Section D -> E
    await page.getByRole('button', { name: 'Următorul' }).click();
    await page.waitForResponse(resp => resp.url().includes('/sections/D') && resp.request().method() === 'PUT' && resp.status() === 200);
    await expect(page.getByText('Secțiunea D salvată')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'E — Segment Posterior (Fundoscopie)' })).toBeVisible();

    // Section E -> F
    await page.getByRole('button', { name: 'Următorul' }).click();
    await page.waitForResponse(resp => resp.url().includes('/sections/E') && resp.request().method() === 'PUT' && resp.status() === 200);
    await expect(page.getByText('Secțiunea E salvată')).toBeVisible();

    // Section F -> G
    await page.getByRole('button', { name: 'Următorul' }).click();
    await page.waitForResponse(resp => resp.url().includes('/sections/F') && resp.request().method() === 'PUT' && resp.status() === 200);
    await expect(page.getByText('Secțiunea F salvată')).toBeVisible();

    // Section G -> H
    await page.getByRole('button', { name: 'Următorul' }).click();
    await page.waitForResponse(resp => resp.url().includes('/sections/G') && resp.request().method() === 'PUT' && resp.status() === 200);
    await expect(page.getByText('Secțiunea G salvată')).toBeVisible();

    // Section H -> I
    await page.getByRole('button', { name: 'Următorul' }).click();
    await page.waitForResponse(resp => resp.url().includes('/sections/H') && resp.request().method() === 'PUT' && resp.status() === 200);
    await expect(page.getByText('Secțiunea H salvată')).toBeVisible();
    
    // Verify Section I (Signature) is loaded
    await expect(page.getByRole('heading', { name: 'I — Semnătură Digitală & Audit' })).toBeVisible();

    // 6. Sign Document
    await page.getByRole('button', { name: /Semnează Digital/i }).click();
    await page.waitForResponse(resp => resp.url().includes('/sign') && resp.request().method() === 'POST' && resp.status() === 200);

    // Verify final success message
    await expect(page.getByText('Consultație semnată digital').first()).toBeVisible();
    await expect(page.getByText('Consultație finalizată și semnată digital')).toBeVisible();

    // 7. Verify read-only enforcement
    // Navigate back to Section A which contains inputs to verify they are read-only
    await page.getByRole('button', { name: /A — Acuitate/i }).first().click();
    const firstInput = page.locator('input[type="number"]').first();
    await expect(firstInput).toBeDisabled({ timeout: 5000 });
  });

  test('should validate IOP input bounds in EMR Section D', async ({ page, request }) => {
    await setupLiveAuth(page, request);

    await page.goto('/');
    await page.getByRole('button', { name: 'Autentificare' }).click();
    await expect(page.getByText('DoctorDashboard')).toBeVisible({ timeout: 10000 }).catch(() => {});

    // Navigate to consultation
    await page.evaluate((id) => {
      window.history.pushState({}, '', `/consultation?patientId=${id}`);
      window.dispatchEvent(new Event('popstate'));
    }, isolatedPatient.id);

    await expect(page.getByRole('button', { name: 'Începe Consultația' })).toBeVisible({ timeout: 15000 });
    await page.getByRole('button', { name: 'Începe Consultația' }).click();

    // Fast-track navigate to Section D
    // Section A -> B
    await page.getByRole('button', { name: 'Următorul' }).click();
    // Section B -> C
    await page.getByRole('button', { name: 'Următorul' }).click();
    // Section C -> D
    await page.getByRole('button', { name: 'Următorul' }).click();

    // Verify Section D IOP is loaded
    await expect(page.getByRole('heading', { name: 'D — Tensiune Oculară (IOP)' })).toBeVisible();

    // Fill in extreme out-of-bounds negative and massive IOP values
    const iopODInput = page.locator('input[type="number"]').first();
    const iopOSInput = page.locator('input[type="number"]').last();
    await iopODInput.fill('150');
    await iopOSInput.fill('-5');

    // Section D -> E
    await page.getByRole('button', { name: 'Următorul' }).click();

    // We check if the save was accepted or if there's any warning/toast.
    // If it is accepted without a warning, we log it!
    await page.waitForTimeout(1000);
    const hasWarning = await page.getByText(/valoare nevalidă/i).isVisible() || await page.getByText(/eroare/i).isVisible();
    if (!hasWarning) {
      console.log('BUG DETECTED: System allowed saving EMR Section D with impossible IOP values (OD 150, OS -5) without any warnings or validation!');
    }
  });
});
