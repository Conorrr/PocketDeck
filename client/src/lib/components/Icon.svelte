<script lang="ts">
	let { name, count = 1, height=4 } = $props();
</script>

{#if name}
    <span class="flex flex-row gap-[3px]">
        {#each Array(count) as _}
            {#await import(`$lib/assets/icons/${name.toLocaleLowerCase()}.webp`) then { default: src }}
                <img class="h-{height}" alt="{name} icon" {src} />
            {/await}
        {/each}
    </span>
{:else}
    <span class="inline-block h-[1em] w-4 translate-y-[3px] animate-pulse rounded-sm bg-gray-200"></span>
{/if}
