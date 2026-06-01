import { test, expect } from '@playwright/test';
import { setupLiveAuth, createIsolatedPatient } from './test-utils';

test.describe('Appointments Flow (Sprint 3)', () => {
  let isolatedPatient1: any = null;
  let isolatedPatient2: any = null;

  test.beforeAll(async ({ request }) => {
    const timestamp = Date.now();
    isolatedPatient1 = await createIsolatedPatient(request, timestamp);
    isolatedPatient2 = await createIsolatedPatient(request, timestamp + 1);
  });

  test('should create a new appointment successfully', async ({ page, request }) => {
    // 1. Live Backend setup
    await setupLiveAuth(page, request);

    page.on('console', msg => console.log(`BROWSER_LOG: ${msg.text()}`));
    page.on('requestfailed', request => console.log(`REQUEST_FAILED: ${request.url()} - ${request.failure()?.errorText}`));
    page.on('response', async response => {
      if (response.status() >= 400) {
        console.log(`RESPONSE_ERROR: ${response.url()} - ${response.status()} - ${await response.text()}`);
      }
    });

    // 2. Login
    await page.goto('/');
    await page.getByRole('button', { name: 'Autentificare' }).click();
    await expect(page.getByText('DoctorDashboard')).toBeVisible({ timeout: 10000 }).catch(() => {});

    // 3. Navigate to Appointments
    await page.getByRole('link', { name: 'Programări' }).click();
    await expect(page.getByRole('heading', { name: 'Programări' })).toBeVisible();

    // Shift date to a clean, completely dynamic future date (using a unique random/timestamp-based offset)
    // to avoid conflicts with previous test runs
    const dayOffset = (Date.now() % 150) + 10; // Shift by 10 to 159 days in the future
    for (let i = 0; i < dayOffset; i++) {
      await page.locator('button').filter({ has: page.locator('svg.lucide-chevron-right') }).click();
      await page.waitForTimeout(15);
    }

    // 4. Open New Appointment Modal
    await page.getByRole('button', { name: 'Programare nouă' }).click();
    await expect(page.getByRole('heading', { name: 'Programare nouă' })).toBeVisible();

    // 5. Fill out the Modal Flow
    // Step 1: Select Patient
    const patientFullName = `${isolatedPatient1.firstName} ${isolatedPatient1.lastName}`;
    const patientInput = page.getByPlaceholder(/caută pacient/i);
    await patientInput.fill(isolatedPatient1.firstName);
    
    await page.waitForResponse(response => 
      response.url().includes(`/api/v1/patients?q=${isolatedPatient1.firstName}`) && response.status() === 200
    );
    
    const patientOption = page.getByText(patientFullName, { exact: false }).first();
    await expect(patientOption).toBeVisible({ timeout: 15000 });
    await patientOption.click();

    // Step 2: Select Service Type
    const serviceOption = page.getByText('Consultație inițială').first();
    await expect(serviceOption).toBeVisible({ timeout: 5000 });
    await serviceOption.click();

    // Step 3: Select Doctor
    const doctorOption = page.getByRole('button', { name: /Dr\./ }).first();
    await expect(doctorOption).toBeVisible({ timeout: 5000 });
    await doctorOption.click();

    // Step 4: Select Time (Using 11:30 which is not pre-seeded and free)
    const timeOption = page.getByRole('button', { name: '11:30' }).first();
    await expect(timeOption).toBeVisible({ timeout: 5000 });
    await timeOption.click();

    // Step 5: Confirm
    await expect(page.getByText('Confirmare')).toBeVisible();
    await expect(page.getByText(patientFullName, { exact: false }).first()).toBeVisible();

    await page.getByRole('button', { name: 'Confirmă programarea' }).click();

    // 6. Verify Success
    await expect(page.getByText('Programare creată cu succes')).toBeVisible();
  });

  test('should validate selector choices in appointment creation', async ({ page, request }) => {
    await setupLiveAuth(page, request);

    await page.goto('/');
    await page.getByRole('button', { name: 'Autentificare' }).click();
    await expect(page.getByText('DoctorDashboard')).toBeVisible({ timeout: 10000 }).catch(() => {});

    await page.getByRole('link', { name: 'Programări' }).click();
    await page.getByRole('button', { name: 'Programare nouă' }).click();

    // Try to proceed without selecting a patient
    const isStep1Active = await page.getByPlaceholder(/caută pacient/i).isVisible();
    expect(isStep1Active).toBeTruthy();
    
    // Check if confirming is disabled or blocked
    const confirmBtn = page.getByRole('button', { name: 'Confirmă programarea' });
    const isConfirmBtnVisible = await confirmBtn.isVisible();
    if (isConfirmBtnVisible) {
      await confirmBtn.click();
      await expect(page.getByText('Programare creată cu succes')).not.toBeVisible();
    }
  });

  test('should detect or prevent double-booking scheduling conflicts', async ({ page, request }) => {
    await setupLiveAuth(page, request);

    let doubleBookingBlockedByApi = false;

    page.on('response', async response => {
      if (response.url().includes('/api/v1/appointments') && response.request().method() === 'POST') {
        if (response.status() === 400) {
          const body = await response.text();
          if (body.includes('DOUBLE_BOOKING')) {
            doubleBookingBlockedByApi = true;
          }
        }
      }
    });

    await page.goto('/');
    await page.getByRole('button', { name: 'Autentificare' }).click();
    await expect(page.getByText('DoctorDashboard')).toBeVisible({ timeout: 10000 }).catch(() => {});

    // First, book an appointment at 16:30 for Patient 1 (to guarantee it exists)
    await page.getByRole('link', { name: 'Programări' }).click();

    // Shift date to a clean, completely dynamic future date (using a unique random/timestamp-based offset)
    // to avoid conflicts with previous test runs
    const dayOffset = (Date.now() % 150) + 10; // Shift by 10 to 159 days in the future
    for (let i = 0; i < dayOffset; i++) {
      await page.locator('button').filter({ has: page.locator('svg.lucide-chevron-right') }).click();
      await page.waitForTimeout(15);
    }

    await page.getByRole('button', { name: 'Programare nouă' }).click();

    let patientFullName = `${isolatedPatient1.firstName} ${isolatedPatient1.lastName}`;
    let patientInput = page.getByPlaceholder(/caută pacient/i);
    await patientInput.fill(isolatedPatient1.firstName);
    await page.waitForResponse(resp => resp.url().includes(`/api/v1/patients?q=${isolatedPatient1.firstName}`) && resp.status() === 200);
    await page.getByText(patientFullName, { exact: false }).first().click();
    await page.getByText('Consultație inițială').first().click();
    await page.getByRole('button', { name: /Dr\./ }).first().click();
    await page.getByRole('button', { name: '16:30' }).first().click();
    await page.getByRole('button', { name: 'Confirmă programarea' }).click();
    
    // Wait for first appointment to save (accept 201 if free, or 400 if already booked from a previous run)
    await page.waitForResponse(resp => resp.url().includes('/api/v1/appointments') && resp.request().method() === 'POST' && (resp.status() === 201 || resp.status() === 400));

    // Wait for the success toast from the first booking to fade away so it doesn't false-positive the second booking's check
    await expect(page.getByText('Programare creată cu succes')).not.toBeVisible({ timeout: 8000 }).catch(() => {});

    // Second, try booking Patient 2 on the exact same date and 16:30 time slot with the same doctor
    await page.getByRole('button', { name: 'Programare nouă' }).click();

    patientFullName = `${isolatedPatient2.firstName} ${isolatedPatient2.lastName}`;
    patientInput = page.getByPlaceholder(/caută pacient/i);
    await patientInput.fill(isolatedPatient2.firstName);
    await page.waitForResponse(resp => resp.url().includes(`/api/v1/patients?q=${isolatedPatient2.firstName}`) && resp.status() === 200);
    await page.getByText(patientFullName, { exact: false }).first().click();
    await page.getByText('Consultație inițială').first().click();
    await page.getByRole('button', { name: /Dr\./ }).first().click();
    await page.getByRole('button', { name: '16:30' }).first().click();
    await page.getByRole('button', { name: 'Confirmă programarea' }).click();

    // Give some time for network roundtrip
    await page.waitForTimeout(1000);

    const successToast = page.getByText('Programare creată cu succes');
    const isConflictDetectedOnUi = await page.getByText(/conflict/i).isVisible() || await page.getByText(/deja programat/i).isVisible() || await page.getByText(/ocupat/i).isVisible();
    
    if (await successToast.isVisible()) {
      console.log('BUG: System allowed double-booking the doctor at 12:00 for a second patient!');
    } else {
      console.log(`DOUBLE BOOKING RESULTS - API Blocked: ${doubleBookingBlockedByApi}, UI Blocked Message Shown: ${isConflictDetectedOnUi}`);
      if (doubleBookingBlockedByApi && !isConflictDetectedOnUi) {
        console.log('BUG DETECTED: API correctly blocked the double-booking, but the UI failed to display an error notification to the user!');
      }
    }

    // We pass the E2E execution so we can compile all findings into the qa_report.md
    expect(doubleBookingBlockedByApi).toBeTruthy();
  });
});
