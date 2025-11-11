<script lang="ts">
	import Icon from './Icon.svelte';

	let { rarity } = $props<{ rarity: string | undefined }>();
	let icon = $derived.by(() => {
		return { name: mapToIcon(rarity), count: count(rarity) };
	});

	function mapToIcon(rarity: string | undefined): string | undefined {
		if (!rarity) {
			return undefined;
		}
		if (rarity == 'c') {
			return 'crown';
		}
		switch (rarity.substring(1)) {
			case 'd':
				return 'diamond';
			case 'sh':
				return 'shiny';
			case 's':
				return 'star';
		}
		return undefined;
	}
	function count(rarity: string | undefined): number {
		if (!rarity) {
			return 0;
		}
		if (rarity == 'p' || rarity == 'c') {
			return 1;
		}
		return parseInt(rarity.substring(0, 1));
	}
</script>

<Icon name={icon.name} count={icon.count} height={4} />
