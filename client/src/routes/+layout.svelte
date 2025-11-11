<script lang="ts">
	import '../app.css';
	import favicon from '$lib/assets/favicon.svg';

	let { children } = $props();
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

<div class="flex min-h-screen bg-slate-100 flex-col bg-[url(/images/bg-left.png)]">
	<nav class="bg-white border-b border-slate-200 shadow-lg z-1 h-12 margin-auto">
		<div class="mx-auto pl-4 flex w-full max-w-6xl items-center justify-between">
			<a href="/" class="block text-2xl text-slate-600 font-bold">PocketDeck.cards</a>

			<div class="flex h-12">
				<a
					href="/decks/upload"
					class="inline-flex items-center px-4 font-semibold text-slate-600 hover:bg-slate-100 hover:text-slate-900"
				>
					Upload Deck
				</a>
			</div>
		</div>
	</nav>

	<main class="mx-auto w-full max-w-6xl z-1 flex-grow px-2 py-5 md:px-0 min-h-100">
		{@render children()}
	</main>

	<footer
		class="w-full bg-white z-1 border-t border-slate-300 shadow-[0_-4px_10px_rgba(0,0,0,0.1)] py-5 h-30"
	>
		<div class="mx-auto flex w-full max-w-4xl items-center justify-center text-slate-500 px-5">
			All Pokémon images, names, characters, and related marks are trademarks and copyright of The
			Pokémon Company, Nintendo, Game Freak, or Creatures Inc. (“Pokémon Rights Holders”). Sharedex
			is an unofficial site and is not endorsed by or affiliated with Pokémon Rights Holders.
		</div>
	</footer>
</div>
