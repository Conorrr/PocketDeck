import { browser } from '$app/environment';
import { OpenPanel } from '@openpanel/web';

export let op: OpenPanel | null = null;

if (browser) {
  op = new OpenPanel({
    apiUrl: 'https://clicks.restall.io', 
    clientId: '8253daad-da2a-4644-bbc5-4c317013363d',
    trackScreenViews: true,
    trackOutgoingLinks: true,
    trackAttributes: true,
  });
}