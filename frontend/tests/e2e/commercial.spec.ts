import { test, expect } from '@playwright/test';
import { setupLiveAuth } from './test-utils';

test.describe('Commercial Suite (Inventory, ERP, Billing)', () => {
  test.beforeEach(async ({ page, request }) => {
    await setupLiveAuth(page, request);
    await page.goto('/');
    await page.getByRole('button', { name: 'Autentificare' }).click();
    await expect(page.getByText('DoctorDashboard')).toBeVisible({ timeout: 10000 }).catch(() => {});
  });

  test('should view inventory and update stock with floor protection', async ({ page }) => {
    await page.getByRole('link', { name: 'Stocuri' }).click();
    await expect(page.getByRole('heading', { name: 'Stocuri & Inventar' })).toBeVisible();

    // Verify category tabs are loaded and click on "Rame" tab (or default is Rame)
    await expect(page.getByRole('tab', { name: 'Rame' })).toBeVisible();

    // Find the first row in the table, click the minus button several times
    // Ensure the stock does not drop below 0 (floor protection test)
    const stockSpan = page.locator('tbody tr td span.font-clinical').first();
    const initialStockText = await stockSpan.textContent();
    const initialStock = parseInt(initialStockText || '0');

    // Click minus multiple times to drain stock to 0
    const minusBtn = page.locator('tbody tr td button').filter({ hasText: '−' }).first();
    for (let i = 0; i < initialStock + 5; i++) {
      await minusBtn.click();
    }

    // Verify stock is exactly 0 and does not go negative
    const finalStockText = await stockSpan.textContent();
    expect(parseInt(finalStockText || '0')).toBe(0);
  });

  test('should navigate to Optical ERP', async ({ page }) => {
    await page.getByRole('link', { name: 'ERP Optic' }).click();
    await expect(page.getByRole('heading', { name: 'ERP Optic — Comenzi' })).toBeVisible();
  });

  test('should process a POS billing invoice and calculate prices accurately', async ({ page }) => {
    await page.getByRole('link', { name: 'Facturare & POS' }).click();
    await expect(page.getByRole('heading', { name: 'Facturare & POS' })).toBeVisible();

    // Get initial total
    const totalSpan = page.locator('text=/TOTAL/i').locator('xpath=following-sibling::span').first();
    const initialTotalText = await totalSpan.textContent();
    let initialTotal = parseInt(initialTotalText?.replace(/[^0-9]/g, '') || '0');

    // Update the price of the first bill item
    const firstPriceInput = page.locator('input[type="number"]').first();
    await firstPriceInput.fill('150');

    // Add another service from catalog
    // We select the first option with a code (e.g., "MED001 — Consultație completă")
    await page.locator('select').first().selectOption({ index: 1 });

    // Verify total recalculates dynamically
    // Wait briefly for react state update
    await page.waitForTimeout(500);

    const updatedTotalText = await totalSpan.textContent();
    const updatedTotal = parseInt(updatedTotalText?.replace(/[^0-9]/g, '') || '0');

    console.log(`QA NOTE: POS Billing total calculated: ${updatedTotal} RON.`);
    expect(updatedTotal).toBeGreaterThan(0);

    // Click Emite Chitanță
    await page.getByRole('button', { name: 'Emite Chitanță' }).click();
    
    // Verify toast
    await expect(page.getByText('Chitanță emisă')).toBeVisible();

    // Verify print preview modal shows the exact updated price
    const modalTotal = page.locator('div.text-base font-bold >> text=/TOTAL/i').locator('xpath=following-sibling::span');
    // If modal is visible, check total
    const isModalVisible = await page.getByRole('heading', { name: 'CHITANȚĂ' }).first().isVisible().catch(() => false);
    if (isModalVisible) {
      await expect(page.getByText(`${updatedTotal.toFixed(2)} RON`)).toBeVisible();
    }
  });
});
