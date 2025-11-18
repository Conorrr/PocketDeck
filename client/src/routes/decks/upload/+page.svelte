<script lang="ts">
	import UploadComponent from "$lib/components/UploadComponent.svelte";
</script>

<svelte:head>
	<title>PocketDeck - Upload</title>
	<meta name="description" content="Easily share decks for Pokemon TCG Pocket. Upload new decks." />
	<meta property="og:title" content="PocketDeck - Upload" />
	<meta property="og:description" content="Easily share decks for Pokemon TCG Pocket. Upload new decks." />
	<meta property="og:type" content="website" />
</svelte:head>

<div class="flex place-items-center content-center justify-center min-h-100 h-[calc(100vh-(var(--spacing)*52))]">
	<UploadComponent />
</div>
