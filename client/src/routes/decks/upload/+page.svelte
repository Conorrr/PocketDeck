<script lang="ts">
	import { goto } from '$app/navigation';
	import { PUBLIC_API_URL } from '$env/static/public';
	import { op } from '$lib/openpanel';
	import type { PageProps } from './$types';

	let file: File | null = null;
	let uploading = $state(false);
	let error: string | null = $state(null);
	let dragging = $state(false);

	const handleSubmit = async (file?: File, uri?: string) => {
		uploading = true;
		error = null;

		try {
			const formData = new FormData();
			if (file) {
				formData.append('screenshot', file);
			}
			if (uri) {
				formData.append('uri', uri);
			}

			const res = await fetch(`${PUBLIC_API_URL}/upload`, {
				method: 'POST',
				body: formData
			});

			const data = await res.json();

			if (!res.ok) {
				if (data.error) {
					error = data.error;
					return;
				}
				op?.track('upload_failed', { responseStatus: res.status });
				throw new Error(`Upload failed: ${res.status} ${res.statusText}`);
			}

			if (!data.deckId) {
				op?.track('incomplete_deck', { uploadId: data.uploadId, totalCards: data.totalCards });
				throw new Error('Unable to find a full deck in the image.');
			}

			op?.track('upload_success', { deckId: data.deckId, uploadId: data.uploadId });
			goto(`/decks/${data.deckId}`);
		} catch (err: any) {
			uploading = false;
			error = err.message ?? 'Unknown error occurred.';
		}
	};

	const onFileChange = (e: Event) => {
		const selected = (e.target as HTMLInputElement).files?.[0];
		if (selected) handleSubmit(selected);
	};

	const handleDragOver = (e: DragEvent) => {
		e.preventDefault();
		dragging = true;
	};

	const handleDragLeave = (e: DragEvent) => {
		e.preventDefault();
		dragging = false;
	};

	const handleDrop = (e: DragEvent) => {
		e.preventDefault();
		dragging = false;

		const dt = e.dataTransfer;
		if (!dt) return;

		const droppedFile = e.dataTransfer?.files?.[0];
		if (droppedFile) {
			handleSubmit(droppedFile);
			op?.track('screenshot_dropped', { droppedFile: droppedFile?.name });
		} else {
			const uri = dt.getData('text/uri-list') || dt.getData('text/plain');
			if (uri && uri.startsWith('http')) {
				handleSubmit(undefined, uri);
				op?.track('screenshot_dropped', { droppedUrl: uri });
			}
		}
	};
</script>

<svelte:head>
	<title>PocketDeck - Upload</title>
	<meta name="description" content="Easily share decks for Pokemon TCG Pocket. Upload new decks." />
	<meta property="og:title" content="PocketDeck - Upload" />
	<meta property="og:description" content="Easily share decks for Pokemon TCG Pocket. Upload new decks." />
	<meta property="og:type" content="website" />
</svelte:head>

{#if dragging}
	<div
		class="absolute z-10 inset-0 bg-slate-200 bg-opacity-30 flex items-center justify-center pointer-events-none"
	>
		<p class="text-slate-700 text-lg font-semibold">Drop your screenshot here!</p>
	</div>
{/if}

<div
	role="region"
	aria-label="Deck screenshot upload drop zone"
	aria-dropeffect="copy"
	aria-live="polite"
	class="flex place-items-center content-center justify-center p-6 min-h-100 h-[calc(100vh-(var(--spacing)*52))]"
	ondragover={handleDragOver}
	ondragleave={handleDragLeave}
	ondrop={handleDrop}
>
	<form
		onsubmit={(e) => {
			e.preventDefault();
			file && handleSubmit(file);
		}}
		class="w-full max-w-md bg-white p-8 opacity-90 rounded-2xl shadow-lg border border-slate-300/50 flex flex-col gap-6"
	>
		<h1 class="text-2xl font-semibold text-slate-800 text-center">Upload a Deck Screenshot</h1>

		<label class="flex flex-col gap-2">
			<span class="text-slate-700 font-medium">Select a file:</span>
			<input
				type="file"
				accept="*/*"
				onchange={onFileChange}
				required
				class="block w-full text-sm text-slate-700 file:mr-4 file:py-2 file:px-4
				       file:rounded-lg file:border-0 file:text-sm file:font-medium
				       file:bg-gray-50 file:gray-slate-700 hover:file:bg-gray-100
				       cursor-pointer border border-slate-300 rounded-lg p-2"
			/>
		</label>
		<p class="text-sm text-slate-500 text-center mt-2">
			You can also <span class="font-medium text-slate-700">drag and drop</span> an image here.
		</p>

		{#if error}
			<p class="text-red-600 text-sm text-center">{error}</p>
		{/if}

		{#if uploading}
			<p class="text-gray-700 text-center">Uploading…</p>
		{/if}
	</form>
</div>
