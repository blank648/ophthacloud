import { APIRequestContext } from '@playwright/test';

let testToken = '';

export async function getLiveToken(request: APIRequestContext) {
  if (testToken) return testToken;
  
  const response = await request.post('http://localhost:8180/realms/ophthacloud/protocol/openid-connect/token', {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    form: {
      client_id: 'ophthacloud-frontend',
      grant_type: 'password',
      username: 'dr.test@clinica-demo.ro',
      password: 'test123'
    }
  });
  
  if (!response.ok()) {
    console.error('Keycloak token fetch failed:', await response.text());
    throw new Error('Failed to fetch token from Keycloak');
  }
  
  const data = await response.json();
  testToken = data.access_token;
  return testToken;
}

export async function createIsolatedPatient(request: APIRequestContext, timestamp: number) {
  const token = await getLiveToken(request);
  const patientData = {
    firstName: `TestPatient_${timestamp}`,
    lastName: `Isolated`,
    dateOfBirth: '1990-01-01',
    gender: 'MALE',
    phone: '0700000000'
  };

  const response = await request.post('http://localhost:8080/api/v1/patients', {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    data: patientData
  });

  if (!response.ok()) {
    throw new Error(`Failed to create isolated patient: ${response.status()} ${response.statusText()} - ${await response.text()}`);
  }

  const result = await response.json();
  return result.data;
}

export async function setupAppointmentType(request: APIRequestContext) {
  const token = await getLiveToken(request);
  const response = await request.get('http://localhost:8080/api/v1/appointment-types', {
    headers: { Authorization: `Bearer ${token}` }
  });
  
  if (response.ok()) {
    const data = await response.json();
    if (data.data && data.data.find((t: any) => t.name === 'Consultație inițială')) {
      return;
    }
  }

  await request.post('http://localhost:8080/api/v1/appointment-types', {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    data: {
      name: 'Consultație inițială',
      colorHex: '#3b82f6',
      durationMinutes: 30,
      description: 'Initial checkup'
    }
  });
}

export async function setupLiveAuth(page: any, request: APIRequestContext) {
  const token = await getLiveToken(request);
  await setupAppointmentType(request);
  await page.route('**/api/v1/**', async (route: any) => {
    const headers = {
      ...route.request().headers(),
      Authorization: `Bearer ${token}`,
    };
    await route.continue({ headers });
  });
}

export async function getLiveTokenForUser(request: APIRequestContext, username: string, password: string) {
  const credentialsToTry = [{ u: username, p: password }];
  if (username === 'admin@ophthacloud.ro') {
    credentialsToTry.push({ u: 'admin@ophthacloud.ro', p: 'parola123' });
    credentialsToTry.push({ u: 'admin.test@clinica-demo.ro', p: 'test123' });
  }
  
  let lastErrorText = '';
  for (const cred of credentialsToTry) {
    try {
      const response = await request.post('http://localhost:8180/realms/ophthacloud/protocol/openid-connect/token', {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        form: {
          client_id: 'ophthacloud-frontend',
          grant_type: 'password',
          username: cred.u,
          password: cred.p
        }
      });
      
      if (response.ok()) {
        const data = await response.json();
        return { token: data.access_token, usernameUsed: cred.u, passwordUsed: cred.p };
      } else {
        lastErrorText = await response.text();
      }
    } catch (e: any) {
      lastErrorText = e.message;
    }
  }
  
  console.error(`Keycloak token fetch failed for ${username}:`, lastErrorText);
  throw new Error(`Failed to fetch token from Keycloak for ${username}`);
}

export async function setupLiveAuthForUser(page: any, request: APIRequestContext, username: string, password: string) {
  const { token } = await getLiveTokenForUser(request, username, password);
  await page.route('**/api/v1/**', async (route: any) => {
    const headers = {
      ...route.request().headers(),
      Authorization: `Bearer ${token}`,
    };
    await route.continue({ headers });
  });
}

export async function performKeycloakLogin(page: any, username: string, password: string) {
  // Determine fallbacks
  const credentialsToTry = [{ u: username, p: password }];
  if (username === 'admin@ophthacloud.ro') {
    credentialsToTry.push({ u: 'admin@ophthacloud.ro', p: 'parola123' });
    credentialsToTry.push({ u: 'admin.test@clinica-demo.ro', p: 'test123' });
  }
  
  // 1. Go to EMR landing page
  await page.goto('http://localhost:5173/');
  
  // 2. Click "Login" button
  await page.getByRole('button', { name: 'Login', exact: true }).click();
  
  // 3. Wait for Keycloak redirect and render of the login form
  await page.waitForSelector('#username', { timeout: 15000 });
  
  // 4. Try first credentials
  await page.locator('#username').fill(credentialsToTry[0].u);
  await page.locator('#password').fill(credentialsToTry[0].p);
  
  // 5. Submit form
  await page.locator('#kc-login').click();
  
  // If there are more credentials and error is visible, try fallbacks
  for (let i = 1; i < credentialsToTry.length; i++) {
    const hasError = await page.locator('.alert-error, #input-error-password, #input-error-username, .alert-error span').first().isVisible({ timeout: 2000 }).catch(() => false);
    if (hasError) {
      console.log(`E2E_LOGIN: Detected login failure for ${username}, retrying with fallback credentials: ${credentialsToTry[i].u}`);
      await page.locator('#username').fill(credentialsToTry[i].u);
      await page.locator('#password').fill(credentialsToTry[i].p);
      await page.locator('#kc-login').click();
    }
  }
  
  // 6. Verify we redirect back successfully to the app and see EMR components
  await page.waitForURL(/.*5173.*/, { timeout: 15000 });
}




