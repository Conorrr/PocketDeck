<script lang="ts">
	import CardImage from './CardImage.svelte';
	import type { Card } from '$lib/models/Card';
	import LoadingText from './util/LoadingText.svelte';
	import { nameExpansion } from '$lib/CardDetailsUtils';
	import { capitalise } from '$lib/Util';
	import Icon from './Icon.svelte';
	import RarityIcon from './RarityIcon.svelte';

	let { cardId = $bindable() } = $props();

	let card: null | Card = $state(null);
	let error = $state(null);

    let expansionsStr = $state([""])
    $effect(() => {
        if(!card) {
            expansionsStr = [""];
        } else {
            expansionsStr = card.expansions.map(nameExpansion);
        }
    })

	async function loadCard() {
		try {
			const res = await fetch(`/api/cards/${cardId}`);
			if (!res.ok) {
				throw new Error(`Failed to load card ${cardId}`);
			}
			card = await res.json();
		} catch (e: any) {
			error = e.message;
		}
	}

	// Load card when component mounts or cardId changes
	$effect(() => {
		if (cardId) {
			loadCard();
		}
	});

    
</script>

<div class="bg-white z-1 sticky top-0 bg-white px-4 py-2 border-b-2 border-slate-400 shadow-xl">
	<h2 class="text-2xl text-slate-700 font-bold">{card?.name || 'Loading...'}</h2>
</div>
<div class="max-w-2xl mx-auto px-6 pb-6 pt-3">
	{#if error}
		<p class="text-red-600 text-center">Error: {error}</p>
	{:else}
		<div class="space-y-3">
			<CardImage
				class="
                    max-w-[367px]
                    shadow-xl
                    rounded-xl
                    overflow-hidden"
				{cardId}
			/>

			<p>
				<strong class="font-semibold">Expansions:</strong>
				<LoadingText value={expansionsStr} width="w-20" />
			</p>
            
            {#if !card || card.rarity !== 'p'}
                <p class="flex items-center gap-1">
                    <strong class="font-semibold">Rarity:</strong>
                    <RarityIcon rarity={card?.rarity} />
                </p>
            {/if}
            {#if !card || card.cardType !== 'pokémon'}
                <p>
                    <strong>Type:</strong>
                    <LoadingText value={capitalise(card?.cardType) + " | " + capitalise(card?.evolutionType)} />
                </p>
            {/if}
			{#if !card || card.cardType == 'pokémon'}
                <p>
                    <strong>Type:</strong>
                    <LoadingText value={capitalise(card?.cardType)} />
                </p>
				<p class="flex items-center gap-1">
					<strong>Energy:</strong>
                    <Icon name={card?.energy} />
				</p>
				<p>
					<strong>HP:</strong>
					<LoadingText value={card?.hp} width="w-10" />
				</p>
			{/if}
            {#if !card || card?.cardType === "pokémon"}
			<p>
				<strong>Evolution:</strong>
				<LoadingText value={capitalise(card?.evolutionType)} />
			</p>
            {/if}
			{#if !card || card.cardType == 'pokémon'}
                <p class="flex items-center gap-1">
                    <strong>Weakness:</strong>
                    {#if card?.weakness == "none"}
                        <small>None</small>
                    {:else}
                        <Icon name={card?.weakness} />
                    {/if}
				</p>
				<p class="flex items-center gap-1">
                    <strong>Retreat:</strong>
                    {#if card?.retreat == "0"}
                        <small>None</small>
                    {:else}
                        <Icon name="colorless" count={card && card?.retreat ? parseInt(card?.retreat) : 1} />
                    {/if}
				</p>
			{/if}

			<div>
				<h3 class="font-semibold mt-4 mb-2">Ability:</h3>
				<ul class="space-y-1">
					{#if card}
						{#if card.ability}
							<li class="p-2 rounded bg-gray-200">
								{#if card.ability.name}
									<strong>{card.ability.name}:</strong>
								{/if}
								<small class="text-gray-600">{card.ability.effect}</small>
							</li>
						{:else}
							<li class="p-2 rounded bg-gray-200">
								<small class="text-gray-600">None</small>
							</li>
						{/if}
					{:else}
						<li class="p-2 rounded bg-gray-50 h-20 animate-pulse bg-gray-200"></li>
					{/if}
				</ul>
			</div>

			{#if !card || card.cardType == 'pokémon'}
				<div>
					<h3 class="font-semibold mt-4 mb-2">Attacks:</h3>
					<ul class="space-y-1">
						{#if card}
							{#each card?.attacks as attack}
								<li class="p-2 rounded bg-gray-200 flex flex-col items-start justify-items-start">
									<span><strong>{attack.name}</strong> - {attack.damage}</span>
									<small class="text-gray-600 flex items-center gap-1">
                                        Cost:
                                        {#if attack && attack.cost.length == 0}
                                            <Icon name={"blank"} count={1} />
                                        {/if}
                                        {#each attack?.cost as cost}
                                            <Icon name={cost} count={1} />
                                        {/each}
                                    </small>
									<small class="text-gray-600">Effect: {attack.effect}</small>
								</li>
							{/each}
						{:else}
							<li class="p-2 rounded bg-gray-50 h-20 animate-pulse bg-gray-200"></li>
						{/if}
					</ul>
				</div>
			{/if}

			<p>
				<strong>Artist:</strong>
				<LoadingText value={card?.artist} />
			</p>
            
			<!-- <div> -->
				<!-- <h3 class="font-semibold mt-4 mb-2">Alternate Versions:</h3> -->

                <p class="flex items-center gap-1">
                    <strong>Alternate Versions:</strong>
                    {#if card?.alternateVersions.length == 0}
                        <small>None</small>
                    {/if}
                </p>

				<div class="grid grid-cols-3 gap-2">
					{#if card}
                        {#each card?.alternateVersions as alt}
                            <CardImage onclick={() => (cardId = alt)} cardId={alt} />
                        {/each}
					{:else}
						<div class="p-2 rounded bg-gray-50 h-60 animate-pulse bg-gray-200"></div>
						<div class="p-2 rounded bg-gray-50 h-60 animate-pulse bg-gray-200"></div>
						<div class="p-2 rounded bg-gray-50 h-60 animate-pulse bg-gray-200"></div>
					{/if}
				</div>
			<!-- </div> -->
		</div>
	{/if}
</div>
