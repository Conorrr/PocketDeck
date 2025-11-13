import { browser } from '$app/environment';
import { OpenPanel } from '@openpanel/web';
import { PUBLIC_OPENPANEL_API_URL, PUBLIC_OPENPANEL_CLIENT_ID } from '$env/static/public';

export let op: OpenPanel | null = null;

if (browser) {
  op = new OpenPanel({
    apiUrl: PUBLIC_OPENPANEL_API_URL, 
    clientId: PUBLIC_OPENPANEL_CLIENT_ID,
    trackScreenViews: true,
    trackOutgoingLinks: true,
    trackAttributes: true,
  });
}