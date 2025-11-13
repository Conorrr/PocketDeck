<script lang="ts">
	import blank from '$lib/assets/icons/blank.webp';
	import colorless from '$lib/assets/icons/colorless.webp';
	import crown from '$lib/assets/icons/crown.webp';
	import darkness from '$lib/assets/icons/darkness.webp';
	import diamond from '$lib/assets/icons/diamond.webp';
	import dragon from '$lib/assets/icons/dragon.webp';
	import fairy from '$lib/assets/icons/fairy.webp';
	import fighting from '$lib/assets/icons/fighting.webp';
	import fire from '$lib/assets/icons/fire.webp';
	import grass from '$lib/assets/icons/grass.webp';
	import lightning from '$lib/assets/icons/lightning.webp';
	import metal from '$lib/assets/icons/metal.webp';
	import psychic from '$lib/assets/icons/psychic.webp';
	import shiny from '$lib/assets/icons/shiny.webp';
	import star from '$lib/assets/icons/star.webp';
	import water from '$lib/assets/icons/water.webp';

	const icons = {
		blank,
		colorless,
		crown,
		darkness,
		diamond,
		dragon,
		fairy,
		fighting,
		fire,
		grass,
		lightning,
		metal,
		psychic,
		shiny,
		star,
		water
	};
	type IconName = keyof typeof icons;

	let {
		name,
		count = 1,
		height = 4
	}: { name?: IconName | null; count?: number; height?: number } = $props<>();
</script>

{#if name}
	<span class="flex flex-row gap-[3px]">
		{#each Array(count) as _}
			<img class="h-{height}" src={icons[name] ?? icons.blank} alt="{name} icon" loading="lazy" />
		{/each}
	</span>
{:else}
	<span class="inline-block h-[1em] w-8 translate-y-[3px] animate-pulse rounded-sm bg-gray-200"
	></span>
{/if}
