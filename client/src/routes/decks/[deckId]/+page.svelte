<script lang="ts">
	import CardImage from '$lib/components/CardImage.svelte';
	import CardDetails from '$lib/components/CardDetails.svelte';
	import Slideover from '$lib/components/util/Slideover.svelte';

	import type { PageProps } from './$types';
	import TiltCard from '$lib/components/util/TiltCard.svelte';
	import { fade } from 'svelte/transition';
	import { op } from '$lib/openpanel';
	import { PUBLIC_API_URL } from '$env/static/public';

	let slideoverOpen = $state(false);
	let slideoverCardId: null | string = $state(null);

	let { params, data }: PageProps = $props();

	function shareClick() {
		navigator.clipboard.writeText(window.location.href);
		copiedPopup = true;
		setTimeout(() => (copiedPopup = false), 4000);
		op?.track('share_click', { deckId: params.deckId });
	}
	var copiedPopup = $state(false);
</script>

<svelte:head>
	<title>{data.title}</title>
	<meta name="description" content={data.description} />
	<meta property="og:title" content={data.title} />
	<meta property="og:description" content={data.description} />
	<meta property="og:image" content={`${PUBLIC_API_URL}/preview/${data.deckId}.webp`} />
	<meta property="og:type" content="website" />
</svelte:head>

<div>
	<div
		class="flex items-center justify-between mb-4 opacity-90 bg-white text-slate-900 p-2 pl-3 rounded-xl shadow-md border border-slate-300/50"
	>
		<h1 class="text-xl font-semibold tracking-tight">
			{data.name}
		</h1>

		<button
			onclick={shareClick}
			id="shareBtn"
			type="button"
			aria-label="Share this page"
			class="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-slate-800 text-white hover:bg-slate-700 active:scale-95 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2"
			title="Share"
		>
			<!-- Share icon -->
			<svg
				xmlns="http://www.w3.org/2000/svg"
				class="w-5 h-5"
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="2"
				aria-hidden="true"
			>
				<path
					d="M4 12v7a1 1 0 001 1h14a1 1 0 001-1v-7"
					stroke-linecap="round"
					stroke-linejoin="round"
				/>
				<path d="M16 6l-4-4-4 4" stroke-linecap="round" stroke-linejoin="round" />
				<path d="M12 2v13" stroke-linecap="round" stroke-linejoin="round" />
			</svg>

			<span class="text-sm font-medium">Share</span>
		</button>
	</div>

	{#if copiedPopup}
		<div
			transition:fade
			class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-10"
		>
			<div
				class="px-4 py-2 bg-slate-600 rounded-md shadow-lg text-white text-sm whitespace-nowrap border-1 border-solid border-slate-700"
			>
				Link Copied!
			</div>
		</div>
	{/if}

	<div
		class="
        grid
        grid-cols-3
        sm:grid-cols-3
        md:grid-cols-4
        lg:grid-cols-5
        xl:grid-cols-5
        gap-3
      "
	>
		{#each data.cards as card}
			<TiltCard>
				<CardImage
					cardId={card.cardId}
					count={card.count}
					name={card.name}
					class="
                        shadow-md
                        rounded-lg
                        overflow-hidden
                    "
					onclick={() => {
						slideoverCardId = card.cardId;
						slideoverOpen = true;
					}}
				/>
			</TiltCard>
		{/each}
	</div>
</div>

<Slideover bind:open={slideoverOpen} key={slideoverCardId}>
	<CardDetails bind:cardId={slideoverCardId} />
</Slideover>
