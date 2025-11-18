<script lang="ts">
	import CardImage from '$lib/components/CardImage.svelte';
	import UploadComponent from '$lib/components/UploadComponent.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<div class="flex flex-col place-items-center gap-3">
	<UploadComponent />

	<div
		class="w-full bg-white py-4 px-6 opacity-90 rounded-2xl shadow-lg border border-slate-300/50"
	>
		<h2 class="text-xl font-semibold text-slate-800 text-left">Latest Decks</h2>
		<div
			class="
        grid
        grid-cols-3
        sm:grid-cols-3
        md:grid-cols-4
        lg:grid-cols-5
        xl:grid-cols-5
        gap-8
        "
		>
			{#each data.latest as deck}
				<a href={`/decks/${deck.deckId}`} class="flex flex-col w-full text-sm">
					<div class="relative group w-full aspect-[4/5] mx-auto hover:z-10 scale-90 my-5">
						{#if deck.topCards.length > 0}
							<div
								class="absolute inset-0 w-full h-full object-cover rounded-xl shadow-lg
                         transform transition-all duration-300 origin-bottom-left
                         {deck.topCards.length > 1 ? "-rotate-8 group-hover:-rotate-6 group-hover:-translate-x-2 group-hover:-translate-y-6" : ""}"
							>
								<CardImage
									cardId={deck.topCards.at(-1)?.cardIds[0]}
									name={deck.topCards.at(-1)?.name}
								/>
							</div>
						{/if}

						{#if deck.topCards.length > 1}
							<div
								class="absolute inset-0 w-full h-full object-cover rounded-xl shadow-lg
                            transform transition-all duration-300 origin-bottom-left
                            {deck.topCards.length > 2 ? "-rotate-4" : ""} group-hover:rotate-3 group-hover:-translate-y-0 z-1"
							>
								<CardImage cardId={deck.topCards.at(-2)?.cardIds[0]} name={deck.topCards.at(-2)?.name} />
							</div>
						{/if}

						{#if deck.topCards.length > 2}
							<div
								class="absolute inset-0 w-full h-full object-cover rounded-xl shadow-lg
                        transform transition-all duration-300 origin-bottom-left
                        group-hover:rotate-12 group-hover:translate-x-2 group-hover:translate-y-6 z-2"
							>
								<CardImage cardId={deck.topCards[0].cardIds[0]} name={deck.topCards[0].name} />
							</div>
						{/if}
					</div>
					<p
						class="mt-2 font-semibold text-center text-base text-gray-800 text-xs
		   group-hover:text-indigo-600 transition-colors duration-300"
					>
						{deck.deckName}
					</p>
				</a>
			{/each}
		</div>
	</div>
</div>
