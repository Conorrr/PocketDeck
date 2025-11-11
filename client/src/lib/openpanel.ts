import { browser } from '$app/environment';
import { OpenPanel } from '@openpanel/web';
import { env } from '$env/dynamic/public';

export let op: OpenPanel | null = null;

if (browser) {
  op = new OpenPanel({
    apiUrl: env.PUBLIC_OPENPANEL_API_URL, 
    clientId: env.PUBLIC_OPENPANEL_CLIENT_ID,
    trackScreenViews: true,
    trackOutgoingLinks: true,
    trackAttributes: true,
  });
}